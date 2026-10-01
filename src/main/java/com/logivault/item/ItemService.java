package com.logivault.item;

import com.logivault.common.exception.BusinessException;
import com.logivault.common.exception.ErrorCode;
import com.logivault.common.web.PageResponse;
import com.logivault.item.dto.CreateItemRequest;
import com.logivault.item.dto.ItemResponse;
import com.logivault.item.dto.ItemSummary;
import com.logivault.item.dto.UpdateItemRequest;
import com.logivault.variant.SkuNormalizer;
import com.logivault.variant.Variant;
import com.logivault.variant.VariantRepository;
import com.logivault.variant.dto.CreateVariantRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ItemService {

    private static final int DEFAULT_MIN_STOCK = 5;

    private final ItemRepository itemRepository;
    private final VariantRepository variantRepository;
    private final ItemMapper itemMapper;

    public ItemService(ItemRepository itemRepository, VariantRepository variantRepository, ItemMapper itemMapper) {
        this.itemRepository = itemRepository;
        this.variantRepository = variantRepository;
        this.itemMapper = itemMapper;
    }

    @Transactional
    public ItemResponse create(CreateItemRequest request) {
        List<CreateVariantRequest> variantRequests = request.variants() == null || request.variants().isEmpty()
                ? List.of(defaultVariantRequest())
                : request.variants();

        List<String> normalizedSkus = variantRequests.stream()
                .map(variant -> SkuNormalizer.normalize(variant.sku()))
                .toList();

        Set<String> uniqueSkus = new HashSet<>(normalizedSkus);
        if (uniqueSkus.size() != normalizedSkus.size()) {
            throw new BusinessException(ErrorCode.SKU_ALREADY_EXISTS, "Duplicate SKU within the request");
        }
        if (variantRepository.existsBySkuIn(uniqueSkus)) {
            throw new BusinessException(ErrorCode.SKU_ALREADY_EXISTS);
        }

        Item item = Item.builder()
                .name(request.name())
                .description(request.description())
                .basePrice(request.basePrice())
                .build();
        itemRepository.save(item);

        for (int i = 0; i < variantRequests.size(); i++) {
            CreateVariantRequest variantRequest = variantRequests.get(i);
            Variant variant = Variant.builder()
                    .item(item)
                    .sku(normalizedSkus.get(i))
                    .name(variantRequest.name())
                    .attributes(variantRequest.attributes() != null ? variantRequest.attributes() : Map.of())
                    .price(variantRequest.price())
                    .minStock(variantRequest.minStock() != null ? variantRequest.minStock() : DEFAULT_MIN_STOCK)
                    .build();
            variantRepository.save(variant);
            item.getVariants().add(variant);
        }

        return itemMapper.toResponse(item);
    }

    @Transactional(readOnly = true)
    public PageResponse<ItemSummary> list(String q, Boolean active, Pageable pageable) {
        String likeQ = (q == null || q.isBlank()) ? null : "%" + q.toLowerCase() + "%";
        Page<ItemSummary> page = itemRepository.search(likeQ, active, pageable);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public ItemResponse getById(UUID id) {
        return itemMapper.toResponse(findWithVariants(id));
    }

    @Transactional
    public ItemResponse update(UUID id, UpdateItemRequest request) {
        Item item = findWithVariants(id);
        item.setName(request.name());
        item.setDescription(request.description());
        item.setBasePrice(request.basePrice());
        return itemMapper.toResponse(item);
    }

    private Item findWithVariants(UUID id) {
        return itemRepository.findWithVariantsById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
    }

    private CreateVariantRequest defaultVariantRequest() {
        String sku = "ITEM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new CreateVariantRequest(sku, "Default", Map.of(), null, null);
    }
}
