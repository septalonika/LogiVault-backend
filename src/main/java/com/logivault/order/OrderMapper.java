package com.logivault.order;

import com.logivault.order.dto.OrderLineResponse;
import com.logivault.order.dto.OrderResponse;
import com.logivault.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface OrderMapper {

    @Mapping(target = "lines", source = "items")
    OrderResponse toResponse(Order order);

    @Mapping(target = "variantId", source = "variant.id")
    @Mapping(target = "sku", source = "variant.sku")
    @Mapping(target = "itemName", source = "variant.item.name")
    @Mapping(target = "variantName", source = "variant.name")
    OrderLineResponse toLine(OrderItem item);

    OrderResponse.OrderActor toActor(User user);
}
