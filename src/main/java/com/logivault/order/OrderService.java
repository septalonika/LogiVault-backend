package com.logivault.order;

import com.logivault.config.LogiVaultProperties;
import com.logivault.dto.PageResponse;
import com.logivault.entity.User;
import com.logivault.exception.BusinessException;
import com.logivault.exception.ErrorCode;
import com.logivault.order.dto.CancelOrderRequest;
import com.logivault.order.dto.CreateOrderRequest;
import com.logivault.order.dto.OrderLineRequest;
import com.logivault.order.dto.OrderResponse;
import com.logivault.order.dto.OrderSummary;
import com.logivault.repository.UserRepository;
import com.logivault.security.CurrentUser;
import com.logivault.stock.MovementType;
import com.logivault.stock.StockService;
import com.logivault.variant.Variant;
import com.logivault.variant.VariantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final VariantRepository variantRepository;
    private final UserRepository userRepository;
    private final StockService stockService;
    private final OrderCodeGenerator codeGenerator;
    private final OrderMapper orderMapper;
    private final Clock clock;
    private final LogiVaultProperties properties;

    public OrderService(OrderRepository orderRepository, VariantRepository variantRepository,
                         UserRepository userRepository, StockService stockService,
                         OrderCodeGenerator codeGenerator, OrderMapper orderMapper, Clock clock,
                         LogiVaultProperties properties) {
        this.orderRepository = orderRepository;
        this.variantRepository = variantRepository;
        this.userRepository = userRepository;
        this.stockService = stockService;
        this.codeGenerator = codeGenerator;
        this.orderMapper = orderMapper;
        this.clock = clock;
        this.properties = properties;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        List<OrderLineRequest> lines = request.lines();
        requireDistinctVariants(lines);

        List<UUID> ids = lines.stream().map(OrderLineRequest::variantId).toList();
        // Rows come back locked and sorted by id, whatever order the client listed the lines in.
        Map<UUID, Variant> variants = variantRepository.findAllByIdForUpdate(ids).stream()
                .collect(Collectors.toMap(Variant::getId, Function.identity()));

        if (variants.size() != ids.size()) {
            List<UUID> missing = ids.stream().filter(id -> !variants.containsKey(id)).toList();
            throw new BusinessException(ErrorCode.VARIANT_NOT_FOUND,
                    "Variants not found: " + missing, Map.of("variantIds", missing));
        }
        requireOrderable(lines, variants);
        requireEnoughStock(lines, variants);

        User actor = userRepository.findById(CurrentUser.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Order order = Order.builder()
                .code(codeGenerator.next())
                .status(OrderStatus.COMPLETED)
                .total(BigDecimal.ZERO)
                .note(request.note())
                .createdBy(actor)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (OrderLineRequest line : lines) {
            Variant variant = variants.get(line.variantId());
            OrderItem item = OrderItem.of(order, variant, line.qty(), variant.getEffectivePrice());
            order.getItems().add(item);
            total = total.add(item.getSubtotal());
        }
        order.setTotal(total);
        orderRepository.save(order);

        for (OrderLineRequest line : lines) {
            stockService.applyMovement(variants.get(line.variantId()), MovementType.SALE, -line.qty(), null, order, actor);
        }
        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse cancel(UUID orderId, CancelOrderRequest request) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        User actor = userRepository.findById(CurrentUser.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        order.cancel(actor, request.reason(), Instant.now(clock));

        // No orderable check here: stock must come back even if the variant was deactivated since (BR-19).
        List<UUID> ids = order.getItems().stream().map(item -> item.getVariant().getId()).toList();
        Map<UUID, Variant> variants = variantRepository.findAllByIdForUpdate(ids).stream()
                .collect(Collectors.toMap(Variant::getId, Function.identity()));
        for (OrderItem item : order.getItems()) {
            stockService.applyMovement(variants.get(item.getVariant().getId()), MovementType.SALE_CANCEL,
                    item.getQty(), request.reason(), order, actor);
        }
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> list(OrderStatus status, LocalDate from, LocalDate to, UUID createdBy,
                                            Pageable pageable) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "from must not be after to");
        }
        var zone = properties.businessZone();
        Instant fromInstant = from == null ? null : from.atStartOfDay(zone).toInstant();
        Instant toExclusive = to == null ? null : to.plusDays(1).atStartOfDay(zone).toInstant();

        Specification<Order> spec = Specification.where(OrderSpecifications.withCreator())
                .and(OrderSpecifications.hasStatus(status))
                .and(OrderSpecifications.createdAtOrAfter(fromInstant))
                .and(OrderSpecifications.createdBefore(toExclusive))
                .and(OrderSpecifications.createdBy(createdBy));
        Page<Order> page = orderRepository.findAll(spec, pageable);

        Map<UUID, Integer> lineCounts = orderRepository
                .countLines(page.getContent().stream().map(Order::getId).toList()).stream()
                .collect(Collectors.toMap(OrderRepository.LineCount::getOrderId, c -> (int) c.getLineCount()));
        return PageResponse.from(page, order -> orderMapper.toSummary(order, lineCounts.getOrDefault(order.getId(), 0)));
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(UUID id) {
        return orderMapper.toResponse(orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND)));
    }

    private void requireDistinctVariants(List<OrderLineRequest> lines) {
        Set<UUID> seen = new HashSet<>();
        for (OrderLineRequest line : lines) {
            if (!seen.add(line.variantId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Duplicate variant in order lines");
            }
        }
    }

    private void requireOrderable(List<OrderLineRequest> lines, Map<UUID, Variant> variants) {
        List<String> inactive = lines.stream()
                .map(line -> variants.get(line.variantId()))
                .filter(variant -> !variant.isOrderable())
                .map(Variant::getSku)
                .toList();
        if (!inactive.isEmpty()) {
            throw new BusinessException(ErrorCode.VARIANT_INACTIVE,
                    "Inactive variants: " + String.join(", ", inactive), Map.of("skus", inactive));
        }
    }

    // Checks every line before failing so the client sees all shortages at once.
    private void requireEnoughStock(List<OrderLineRequest> lines, Map<UUID, Variant> variants) {
        List<Map<String, Object>> shortages = lines.stream()
                .filter(line -> variants.get(line.variantId()).getStock() < line.qty())
                .map(line -> {
                    Variant variant = variants.get(line.variantId());
                    Map<String, Object> shortage = new LinkedHashMap<>();
                    shortage.put("variantId", variant.getId());
                    shortage.put("sku", variant.getSku());
                    shortage.put("requested", line.qty());
                    shortage.put("available", variant.getStock());
                    return shortage;
                })
                .toList();
        if (!shortages.isEmpty()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, ErrorCode.INSUFFICIENT_STOCK.getDefaultMessage(),
                    Map.of("lines", shortages));
        }
    }
}
