package com.logivault.variant;

import com.logivault.common.exception.BusinessException;
import com.logivault.common.exception.ErrorCode;
import com.logivault.item.Item;
import com.logivault.item.ItemRepository;
import com.logivault.variant.dto.CreateVariantRequest;
import com.logivault.variant.dto.UpdateVariantRequest;
import com.logivault.variant.dto.VariantDetailResponse;
import com.logivault.variant.dto.VariantResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class VariantService {

    // Deferred to T-13: once StockMovementRepository exists, changing the SKU after a variant has
    // movements must be rejected with SKU_IMMUTABLE. No movements can exist before T-13 ships.
    private static final int DEFAULT_MIN_STOCK = 5;

    private final ItemRepository itemRepository;
    private final VariantRepository variantRepository;
    private final VariantMapper variantMapper;

    public VariantService(ItemRepository itemRepository, VariantRepository variantRepository, VariantMapper variantMapper) {
        this.itemRepository = itemRepository;
        this.variantRepository = variantRepository;
        this.variantMapper = variantMapper;
    }

    @Transactional(readOnly = true)
    public List<VariantResponse> listByItem(UUID itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new BusinessException(ErrorCode.ITEM_NOT_FOUND);
        }
        return variantRepository.findByItemIdOrderBySku(itemId).stream()
                .map(variantMapper::toResponse)
                .toList();
    }

    @Transactional
    public VariantResponse addVariant(UUID itemId, CreateVariantRequest request) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));

        String sku = SkuNormalizer.normalize(request.sku());
        if (variantRepository.findBySku(sku).isPresent()) {
            throw new BusinessException(ErrorCode.SKU_ALREADY_EXISTS);
        }

        Variant variant = Variant.builder()
                .item(item)
                .sku(sku)
                .name(request.name())
                .attributes(request.attributes() != null ? request.attributes() : Map.of())
                .price(request.price())
                .minStock(request.minStock() != null ? request.minStock() : DEFAULT_MIN_STOCK)
                .build();
        variantRepository.save(variant);

        return variantMapper.toResponse(variant);
    }

    @Transactional(readOnly = true)
    public VariantDetailResponse getById(UUID id) {
        return variantMapper.toDetailResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public VariantDetailResponse getBySku(String sku) {
        Variant variant = variantRepository.findBySku(SkuNormalizer.normalize(sku))
                .orElseThrow(() -> new BusinessException(ErrorCode.VARIANT_NOT_FOUND));
        return variantMapper.toDetailResponse(variant);
    }

    @Transactional
    public VariantResponse update(UUID id, UpdateVariantRequest request) {
        Variant variant = findOrThrow(id);

        String newSku = SkuNormalizer.normalize(request.sku());
        if (!newSku.equals(variant.getSku()) && variantRepository.findBySku(newSku).isPresent()) {
            throw new BusinessException(ErrorCode.SKU_ALREADY_EXISTS);
        }

        variant.setSku(newSku);
        variant.setName(request.name());
        variant.setAttributes(request.attributes() != null ? request.attributes() : Map.of());
        variant.setPrice(request.price());
        variant.setMinStock(request.minStock() != null ? request.minStock() : DEFAULT_MIN_STOCK);

        return variantMapper.toResponse(variant);
    }

    @Transactional
    public void deactivate(UUID id) {
        findOrThrow(id).setActive(false);
    }

    @Transactional
    public VariantResponse activate(UUID id) {
        Variant variant = findOrThrow(id);
        variant.setActive(true);
        return variantMapper.toResponse(variant);
    }

    private Variant findOrThrow(UUID id) {
        return variantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VARIANT_NOT_FOUND));
    }
}
