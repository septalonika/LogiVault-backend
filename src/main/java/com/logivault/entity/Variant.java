package com.logivault.entity;

import com.logivault.entity.BaseEntity;
import com.logivault.entity.Item;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Variant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false, length = 100)
    private String name;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, String> attributes = new LinkedHashMap<>();

    @Column(precision = 14, scale = 2)
    private BigDecimal price;

    // Stock only ever changes through StockService.applyMovement, never a plain setter.
    @Setter(AccessLevel.NONE)
    @Column(nullable = false)
    private int stock;

    @Column(name = "min_stock", nullable = false)
    private int minStock;

    @Version
    private long version;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    public BigDecimal getEffectivePrice() {
        return price != null ? price : item.getBasePrice();
    }

    public boolean isLowStock() {
        return stock <= minStock;
    }

    public boolean isOrderable() {
        return active && item.isActive();
    }

    // Only StockService.applyMovement may call this; it is the single path that changes stock.
    public void applyStockDelta(int delta) {
        this.stock += delta;
    }
}
