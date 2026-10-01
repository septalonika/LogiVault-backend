package com.logivault.repository;

import com.logivault.entity.Order;
import com.logivault.entity.OrderStatus;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<Order> withCreator() {
        return (root, query, cb) -> {
            // The count query has no entity to fetch into, so only fetch for the content query.
            if (query != null && !Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                root.fetch("createdBy", JoinType.LEFT);
            }
            return null;
        };
    }

    public static Specification<Order> hasStatus(OrderStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Order> createdAtOrAfter(Instant from) {
        return from == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Order> createdBefore(Instant toExclusive) {
        return toExclusive == null ? null : (root, query, cb) -> cb.lessThan(root.get("createdAt"), toExclusive);
    }

    public static Specification<Order> createdBy(UUID userId) {
        return userId == null ? null : (root, query, cb) -> cb.equal(root.get("createdBy").get("id"), userId);
    }
}
