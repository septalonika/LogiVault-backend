package com.logivault.order;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") UUID id);

    @Override
    @EntityGraph(attributePaths = {"items", "items.variant", "items.variant.item", "createdBy", "cancelledBy"})
    Optional<Order> findById(UUID id);

    // One grouped query for a whole page of orders, instead of touching each order's lazy items.
    @Query("select i.order.id as orderId, count(i) as lineCount from OrderItem i where i.order.id in :ids group by i.order.id")
    List<LineCount> countLines(@Param("ids") Collection<UUID> ids);

    interface LineCount {
        UUID getOrderId();

        long getLineCount();
    }
}
