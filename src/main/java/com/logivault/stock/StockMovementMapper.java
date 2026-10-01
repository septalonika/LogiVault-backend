package com.logivault.stock;

import com.logivault.stock.dto.MovementResponse;
import com.logivault.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface StockMovementMapper {

    @Mapping(target = "orderCode", source = "order.code")
    MovementResponse toResponse(StockMovement movement);

    MovementResponse.MovementActor toActor(User user);
}
