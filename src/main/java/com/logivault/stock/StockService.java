package com.logivault.stock;

import com.logivault.common.exception.BusinessException;
import com.logivault.common.exception.ErrorCode;
import com.logivault.common.security.CurrentUser;
import com.logivault.stock.dto.AdjustStockRequest;
import com.logivault.stock.dto.StockChangeResponse;
import com.logivault.stock.dto.StockInRequest;
import com.logivault.user.User;
import com.logivault.user.UserRepository;
import com.logivault.variant.Variant;
import com.logivault.variant.VariantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class StockService {

    private final StockMovementRepository stockMovementRepository;
    private final VariantRepository variantRepository;
    private final UserRepository userRepository;
    private final StockMovementMapper stockMovementMapper;
    private final Clock clock;

    public StockService(StockMovementRepository stockMovementRepository, VariantRepository variantRepository,
                         UserRepository userRepository, StockMovementMapper stockMovementMapper, Clock clock) {
        this.stockMovementRepository = stockMovementRepository;
        this.variantRepository = variantRepository;
        this.userRepository = userRepository;
        this.stockMovementMapper = stockMovementMapper;
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

    @Transactional
    public StockChangeResponse stockIn(UUID variantId, StockInRequest request) {
        Variant variant = lockOrThrow(variantId);
        StockMovement movement = applyMovement(variant, MovementType.STOCK_IN, request.qty(), request.note(), null, currentActor());
        return toChangeResponse(variant, movement);
    }

    @Transactional
    public StockChangeResponse adjust(UUID variantId, AdjustStockRequest request) {
        if (request.delta() == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "delta must not be zero");
        }
        Variant variant = lockOrThrow(variantId);
        StockMovement movement = applyMovement(variant, MovementType.ADJUSTMENT, request.delta(), request.reason(), null, currentActor());
        return toChangeResponse(variant, movement);
    }

    private Variant lockOrThrow(UUID variantId) {
        List<Variant> locked = variantRepository.findAllByIdForUpdate(List.of(variantId));
        if (locked.isEmpty()) {
            throw new BusinessException(ErrorCode.VARIANT_NOT_FOUND);
        }
        return locked.get(0);
    }

    private User currentActor() {
        return userRepository.findById(CurrentUser.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private StockChangeResponse toChangeResponse(Variant variant, StockMovement movement) {
        return new StockChangeResponse(variant.getId(), variant.getSku(), variant.getStock(),
                stockMovementMapper.toResponse(movement));
    }
}
