package com.logivault.variant;

import com.logivault.item.Item;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class VariantTest {

    @Test
    void getEffectivePrice_fallsBackToItemBasePrice_whenVariantHasNoOwnPrice() {
        Item item = Item.builder().basePrice(new BigDecimal("85000.00")).build();
        Variant variant = Variant.builder().item(item).build();

        assertThat(variant.getEffectivePrice()).isEqualByComparingTo("85000.00");
    }

    @Test
    void getEffectivePrice_usesVariantPrice_whenSet() {
        Item item = Item.builder().basePrice(new BigDecimal("85000.00")).build();
        Variant variant = Variant.builder().item(item).price(new BigDecimal("95000.00")).build();

        assertThat(variant.getEffectivePrice()).isEqualByComparingTo("95000.00");
    }

    @Test
    void isLowStock_isTrue_whenStockEqualsMinStock() {
        Variant variant = Variant.builder().stock(5).minStock(5).build();

        assertThat(variant.isLowStock()).isTrue();
    }

    @Test
    void isLowStock_isFalse_whenStockAboveMinStock() {
        Variant variant = Variant.builder().stock(6).minStock(5).build();

        assertThat(variant.isLowStock()).isFalse();
    }

    @Test
    void isOrderable_requiresBothVariantAndItemActive() {
        Item activeItem = Item.builder().active(true).build();
        Item inactiveItem = Item.builder().active(false).build();

        assertThat(Variant.builder().item(activeItem).active(true).build().isOrderable()).isTrue();
        assertThat(Variant.builder().item(activeItem).active(false).build().isOrderable()).isFalse();
        assertThat(Variant.builder().item(inactiveItem).active(true).build().isOrderable()).isFalse();
    }
}
