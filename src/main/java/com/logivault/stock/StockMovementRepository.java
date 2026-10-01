package com.logivault.stock;

import org.springframework.data.repository.Repository;

import java.util.UUID;

// Bare Repository, not JpaRepository: no update/delete method exists here (BR-05).
public interface StockMovementRepository extends Repository<StockMovement, UUID> {

    StockMovement save(StockMovement movement);

    boolean existsByVariantId(UUID variantId);
}
