package com.logivault.mapper;

import com.logivault.dto.variant.VariantDetailResponse;
import com.logivault.dto.variant.VariantResponse;
import com.logivault.entity.Variant;
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
