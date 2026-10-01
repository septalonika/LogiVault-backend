package com.logivault.mapper;

import com.logivault.dto.item.ItemResponse;
import com.logivault.entity.Item;
import com.logivault.mapper.VariantMapper;
import org.mapstruct.Mapper;

@Mapper(uses = VariantMapper.class)
public interface ItemMapper {

    ItemResponse toResponse(Item item);
}
