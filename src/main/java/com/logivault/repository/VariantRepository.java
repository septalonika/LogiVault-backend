package com.logivault.repository;

import com.logivault.dto.stock.LowStockResponse;
import com.logivault.entity.Variant;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // Most critical first; the order is part of the query, so the pageable must carry no sort.
    @Query(value = """
            select new com.logivault.dto.stock.LowStockResponse(v.id, v.sku, i.name, v.name, v.stock, v.minStock)
            from Variant v join v.item i
            where v.active = true and i.active = true and v.stock <= v.minStock
            order by (v.stock - v.minStock) asc, v.sku asc
            """,
            countQuery = """
            select count(v) from Variant v join v.item i
            where v.active = true and i.active = true and v.stock <= v.minStock
            """)
    Page<LowStockResponse> findLowStock(Pageable pageable);
}
