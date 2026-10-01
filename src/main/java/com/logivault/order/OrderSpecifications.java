package com.logivault.order;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

final class OrderSpecifications {

    private OrderSpecifications() {
    }

    static Specification<Order> withCreator() {
        return (root, query, cb) -> {
            // The count query has no entity to fetch into, so only fetch for the content query.
            if (query != null && !Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                root.fetch("createdBy", JoinType.LEFT);
            }
            return null;
        };
    }

    static Specification<Order> hasStatus(OrderStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    static Specification<Order> createdAtOrAfter(Instant from) {
        return from == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    static Specification<Order> createdBefore(Instant toExclusive) {
        return toExclusive == null ? null : (root, query, cb) -> cb.lessThan(root.get("createdAt"), toExclusive);
    }

    static Specification<Order> createdBy(UUID userId) {
        return userId == null ? null : (root, query, cb) -> cb.equal(root.get("createdBy").get("id"), userId);
    }
}
