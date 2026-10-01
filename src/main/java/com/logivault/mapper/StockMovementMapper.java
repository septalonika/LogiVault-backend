package com.logivault.mapper;

import com.logivault.dto.stock.MovementResponse;
import com.logivault.entity.StockMovement;
import com.logivault.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface StockMovementMapper {

    @Mapping(target = "orderCode", source = "order.code")
    MovementResponse toResponse(StockMovement movement);

    MovementResponse.MovementActor toActor(User user);
}
