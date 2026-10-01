package com.logivault.stock;

import com.logivault.stock.dto.MovementResponse;
import com.logivault.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface StockMovementMapper {

    // No Order entity yet, so orderCode can't be resolved here.
    @Mapping(target = "orderCode", ignore = true)
    MovementResponse toResponse(StockMovement movement);

    MovementResponse.MovementActor toActor(User user);
}
