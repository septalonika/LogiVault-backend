package com.logivault.variant;

import com.logivault.variant.dto.VariantDetailResponse;
import com.logivault.variant.dto.VariantResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface VariantMapper {

    VariantResponse toResponse(Variant variant);

    @Mapping(target = "itemId", source = "item.id")
    @Mapping(target = "itemName", source = "item.name")
    @Mapping(target = "itemActive", source = "item.active")
    VariantDetailResponse toDetailResponse(Variant variant);
}
