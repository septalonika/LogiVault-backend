package com.logivault.variant;

import com.logivault.item.Item;
import com.logivault.item.ItemRepository;
import com.logivault.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VariantRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private VariantRepository variantRepository;

    @Test
    void attributesJsonb_roundTripsThroughPostgres() {
        Item item = itemRepository.save(Item.builder().name("Basic T-Shirt").basePrice(new BigDecimal("85000.00")).build());

        Variant saved = variantRepository.saveAndFlush(Variant.builder()
                .item(item)
                .sku(uniqueSku())
                .name("Red / M")
                .attributes(Map.of("color", "Red", "size", "M"))
                .minStock(5)
                .build());

        Variant reloaded = variantRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getAttributes()).containsExactlyInAnyOrderEntriesOf(Map.of("color", "Red", "size", "M"));
    }

    @Test
    void findBySkuAndExistsBySkuIn_matchExactSku() {
        Item item = itemRepository.save(Item.builder().name("Item").basePrice(BigDecimal.TEN).build());
        String sku = uniqueSku();
        variantRepository.saveAndFlush(Variant.builder().item(item).sku(sku).name("V").minStock(5).build());

        assertThat(variantRepository.findBySku(sku)).isPresent();
        assertThat(variantRepository.existsBySkuIn(List.of(sku, "does-not-exist"))).isTrue();
        assertThat(variantRepository.existsBySkuIn(List.of("does-not-exist"))).isFalse();
    }

    @Test
    void findByItemIdOrderBySku_returnsVariantsSortedBySku() {
        Item item = itemRepository.save(Item.builder().name("Item").basePrice(BigDecimal.TEN).build());
        String suffix = "-" + System.nanoTime();
        variantRepository.saveAndFlush(Variant.builder().item(item).sku("B" + suffix).name("B").minStock(5).build());
        variantRepository.saveAndFlush(Variant.builder().item(item).sku("A" + suffix).name("A").minStock(5).build());

        List<Variant> variants = variantRepository.findByItemIdOrderBySku(item.getId());

        assertThat(variants).extracting(Variant::getSku).containsExactly("A" + suffix, "B" + suffix);
    }

    private static String uniqueSku() {
        return "SKU-" + System.nanoTime();
    }
}
