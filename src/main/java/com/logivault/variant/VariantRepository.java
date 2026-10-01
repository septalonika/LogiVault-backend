package com.logivault.variant;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VariantRepository extends JpaRepository<Variant, UUID> {

    // Locks rows in ascending id order so two orders touching overlapping variants can't deadlock (BR-03).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Variant v join fetch v.item where v.id in :ids order by v.id")
    List<Variant> findAllByIdForUpdate(@Param("ids") Collection<UUID> ids);

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
