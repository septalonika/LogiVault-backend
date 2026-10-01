package com.logivault.variant;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VariantRepository extends JpaRepository<Variant, UUID> {

    Optional<Variant> findBySku(String sku);

    boolean existsBySkuIn(Collection<String> skus);

    // Fetches the parent item too, since VariantResponse.effectivePrice needs it (avoids N+1).
    @EntityGraph(attributePaths = "item")
    List<Variant> findByItemIdOrderBySku(UUID itemId);
}
