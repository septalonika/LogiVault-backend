package com.logivault.stock;

import com.logivault.common.config.LogiVaultProperties;
import com.logivault.common.exception.BusinessException;
import com.logivault.common.exception.ErrorCode;
import com.logivault.common.security.CurrentUser;
import com.logivault.common.web.PageResponse;
import com.logivault.order.Order;
import com.logivault.stock.dto.AdjustStockRequest;
import com.logivault.stock.dto.MovementResponse;
import com.logivault.stock.dto.StockChangeResponse;
import com.logivault.stock.dto.StockInRequest;
import com.logivault.user.User;
import com.logivault.user.UserRepository;
import com.logivault.variant.Variant;
import com.logivault.variant.VariantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class StockService {

    private final StockMovementRepository stockMovementRepository;
    private final VariantRepository variantRepository;
    private final UserRepository userRepository;
    private final StockMovementMapper stockMovementMapper;
    private final LogiVaultProperties properties;
    private final Clock clock;

    public StockService(StockMovementRepository stockMovementRepository, VariantRepository variantRepository,
                         UserRepository userRepository, StockMovementMapper stockMovementMapper,
                         LogiVaultProperties properties, Clock clock) {
        this.stockMovementRepository = stockMovementRepository;
        this.variantRepository = variantRepository;
        this.userRepository = userRepository;
        this.stockMovementMapper = stockMovementMapper;
        this.properties = properties;
        this.clock = clock;
    }

    // The only path that changes a variant's stock. Requires an existing transaction (caller must
    // have already locked the variant), so the stock change and its ledger entry commit together.
    @Transactional(propagation = Propagation.MANDATORY)
    public StockMovement applyMovement(Variant lockedVariant, MovementType type, int signedQty, String reason,
                                        Order order, User actor) {
        int before = lockedVariant.getStock();
        int after = before + signedQty;
        if (after < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }

        lockedVariant.applyStockDelta(signedQty);
        StockMovement movement = stockMovementRepository.save(
                StockMovement.of(lockedVariant, type, signedQty, before, after, reason, order, actor, Instant.now(clock)));

        log.info("Stock {} on variant {}: {} -> {} ({})", type, lockedVariant.getId(), before, after, actor.getId());
        return movement;
    }

    @Transactional
    public StockChangeResponse stockIn(UUID variantId, StockInRequest request) {
        Variant variant = lockOrThrow(variantId);
        StockMovement movement = applyMovement(variant, MovementType.STOCK_IN, request.qty(), request.note(), null, currentActor());
        return toChangeResponse(variant, movement);
    }

    @Transactional
    public StockChangeResponse adjust(UUID variantId, AdjustStockRequest request) {
        if (request.delta() == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "delta must not be zero");
        }
        Variant variant = lockOrThrow(variantId);
        StockMovement movement = applyMovement(variant, MovementType.ADJUSTMENT, request.delta(), request.reason(), null, currentActor());
        return toChangeResponse(variant, movement);
    }

    @Transactional(readOnly = true)
    public PageResponse<MovementResponse> history(UUID variantId, MovementType type, LocalDate from, LocalDate to,
                                                   Pageable pageable) {
        if (!variantRepository.existsById(variantId)) {
            throw new BusinessException(ErrorCode.VARIANT_NOT_FOUND);
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "from must not be after to");
        }

        var zone = properties.businessZone();
        Instant fromInstant = from == null ? null : from.atStartOfDay(zone).toInstant();
        Instant toExclusive = to == null ? null : to.plusDays(1).atStartOfDay(zone).toInstant();

        // Sort is fixed to newest-first; the client's own sort request (if any) is ignored here.
        Pageable fixedSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<StockMovement> page = stockMovementRepository.findHistory(variantId,
                type != null, type, fromInstant != null, fromInstant, toExclusive != null, toExclusive, fixedSort);
        return PageResponse.from(page, stockMovementMapper::toResponse);
    }

    private Variant lockOrThrow(UUID variantId) {
        List<Variant> locked = variantRepository.findAllByIdForUpdate(List.of(variantId));
        if (locked.isEmpty()) {
            throw new BusinessException(ErrorCode.VARIANT_NOT_FOUND);
        }
        return locked.get(0);
    }

    private User currentActor() {
        return userRepository.findById(CurrentUser.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private StockChangeResponse toChangeResponse(Variant variant, StockMovement movement) {
        return new StockChangeResponse(variant.getId(), variant.getSku(), variant.getStock(),
                stockMovementMapper.toResponse(movement));
    }
}
