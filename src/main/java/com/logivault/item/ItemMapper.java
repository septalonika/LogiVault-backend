package com.logivault.item;

import com.logivault.item.dto.ItemResponse;
import com.logivault.variant.VariantMapper;
import org.mapstruct.Mapper;

@Mapper(uses = VariantMapper.class)
public interface ItemMapper {

    ItemResponse toResponse(Item item);
}
