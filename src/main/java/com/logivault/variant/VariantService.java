package com.logivault.variant;

import com.logivault.common.exception.BusinessException;
import com.logivault.common.exception.ErrorCode;
import com.logivault.item.Item;
import com.logivault.item.ItemRepository;
import com.logivault.variant.dto.CreateVariantRequest;
import com.logivault.variant.dto.VariantResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class VariantService {

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
}
