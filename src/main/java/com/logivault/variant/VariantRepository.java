package com.logivault.variant;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VariantRepository extends JpaRepository<Variant, UUID> {

    // Fetches the parent item too, since effectivePrice/VariantDetailResponse need it (avoids N+1).
    @EntityGraph(attributePaths = "item")
    Optional<Variant> findBySku(String sku);

    @Override
    @EntityGraph(attributePaths = "item")
    Optional<Variant> findById(UUID id);

    boolean existsBySkuIn(Collection<String> skus);

    @EntityGraph(attributePaths = "item")
    List<Variant> findByItemIdOrderBySku(UUID itemId);
}
