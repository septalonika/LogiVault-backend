package com.logivault.stock;

import com.logivault.common.exception.BusinessException;
import com.logivault.common.exception.ErrorCode;
import com.logivault.user.User;
import com.logivault.variant.Variant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class StockService {

    private final StockMovementRepository stockMovementRepository;
    private final Clock clock;

    public StockService(StockMovementRepository stockMovementRepository, Clock clock) {
        this.stockMovementRepository = stockMovementRepository;
        this.clock = clock;
    }

    // The only path that changes a variant's stock. Requires an existing transaction (caller must
    // have already locked the variant), so the stock change and its ledger entry commit together.
    @Transactional(propagation = Propagation.MANDATORY)
    public StockMovement applyMovement(Variant lockedVariant, MovementType type, int signedQty, String reason,
                                        UUID orderId, User actor) {
        int before = lockedVariant.getStock();
        int after = before + signedQty;
        if (after < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }

        lockedVariant.applyStockDelta(signedQty);
        StockMovement movement = stockMovementRepository.save(
                StockMovement.of(lockedVariant, type, signedQty, before, after, reason, orderId, actor, Instant.now(clock)));

        log.info("Stock {} on variant {}: {} -> {} ({})", type, lockedVariant.getId(), before, after, actor.getId());
        return movement;
    }
}
