package com.logivault.variant;

import com.logivault.variant.dto.VariantResponse;
import org.mapstruct.Mapper;

@Mapper
public interface VariantMapper {

    VariantResponse toResponse(Variant variant);
}
