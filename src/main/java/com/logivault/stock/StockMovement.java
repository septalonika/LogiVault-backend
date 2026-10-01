package com.logivault.stock;

import com.logivault.entity.User;
import com.logivault.entity.Variant;
import com.logivault.order.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

// Append-only ledger: no setters, no update/delete repository method (BR-05).
@Entity
@Table(name = "stock_movements")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private Variant variant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private MovementType type;

    @Column(nullable = false)
    private int qty;

    @Column(name = "stock_before", nullable = false)
    private int stockBefore;

    @Column(name = "stock_after", nullable = false)
    private int stockAfter;

    @Column(length = 255)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static StockMovement of(Variant variant, MovementType type, int qty, int stockBefore, int stockAfter,
                                    String reason, Order order, User actor, Instant createdAt) {
        return StockMovement.builder()
                .variant(variant)
                .type(type)
                .qty(qty)
                .stockBefore(stockBefore)
                .stockAfter(stockAfter)
                .reason(reason)
                .order(order)
                .actor(actor)
                .createdAt(createdAt)
                .build();
    }
}
