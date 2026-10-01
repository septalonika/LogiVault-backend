package com.logivault.mapper;

import com.logivault.dto.order.OrderLineResponse;
import com.logivault.dto.order.OrderResponse;
import com.logivault.dto.order.OrderSummary;
import com.logivault.entity.Order;
import com.logivault.entity.OrderItem;
import com.logivault.entity.User;
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

    @Mapping(target = "itemCount", source = "itemCount")
    OrderSummary toSummary(Order order, int itemCount);

    OrderResponse.OrderActor toActor(User user);
}
