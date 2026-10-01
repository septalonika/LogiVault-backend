package com.logivault.repository;

import com.logivault.entity.MovementType;
import com.logivault.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

// Bare Repository, not JpaRepository: no update/delete method exists here (BR-05).
public interface StockMovementRepository extends Repository<StockMovement, UUID> {

    StockMovement save(StockMovement movement);

    boolean existsByVariantId(UUID variantId);

    // Each optional filter gets its own boolean flag instead of "(:param is null or ...)": a parameter
    // used only in an IS NULL check has no type context, and Postgres then refuses to bind it.
    @EntityGraph(attributePaths = {"actor", "order"})
    @Query("""
            select m from StockMovement m
            where m.variant.id = :variantId
              and (:hasType = false or m.type = :type)
              and (:hasFrom = false or m.createdAt >= :from)
              and (:hasTo = false or m.createdAt < :toExclusive)
            order by m.createdAt desc
            """)
    Page<StockMovement> findHistory(@Param("variantId") UUID variantId,
                                     @Param("hasType") boolean hasType, @Param("type") MovementType type,
                                     @Param("hasFrom") boolean hasFrom, @Param("from") Instant from,
                                     @Param("hasTo") boolean hasTo, @Param("toExclusive") Instant toExclusive,
                                     Pageable pageable);
}
