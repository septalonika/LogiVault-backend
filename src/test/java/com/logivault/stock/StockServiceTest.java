package com.logivault.stock;

import com.logivault.common.exception.BusinessException;
import com.logivault.common.exception.ErrorCode;
import com.logivault.item.Item;
import com.logivault.user.Role;
import com.logivault.user.User;
import com.logivault.variant.Variant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StockServiceTest {

    private final StockMovementRepository stockMovementRepository = mock(StockMovementRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final StockService stockService = new StockService(stockMovementRepository, clock);

    @Test
    void applyMovement_withEnoughStock_updatesVariantAndSavesMovement() {
        Variant variant = variantWithStock(5);
        User actor = user();
        when(stockMovementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StockMovement result = stockService.applyMovement(variant, MovementType.STOCK_IN, 3, null, null, actor);

        assertThat(variant.getStock()).isEqualTo(8);
        assertThat(result.getStockBefore()).isEqualTo(5);
        assertThat(result.getStockAfter()).isEqualTo(8);
        assertThat(result.getQty()).isEqualTo(3);
        assertThat(result.getType()).isEqualTo(MovementType.STOCK_IN);
        assertThat(result.getActor()).isEqualTo(actor);

        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        org.mockito.Mockito.verify(stockMovementRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedAt()).isEqualTo(Instant.now(clock));
    }

    @Test
    void applyMovement_belowZero_throwsAndDoesNotSave() {
        Variant variant = variantWithStock(5);

        assertThatThrownBy(() -> stockService.applyMovement(variant, MovementType.SALE, -6, null, null, user()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_STOCK);

        assertThat(variant.getStock()).isEqualTo(5);
        verifyNoInteractions(stockMovementRepository);
    }

    @Test
    void applyMovement_exactlyToZero_isAllowed() {
        Variant variant = variantWithStock(5);
        when(stockMovementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StockMovement result = stockService.applyMovement(variant, MovementType.SALE, -5, null, null, user());

        assertThat(variant.getStock()).isZero();
        assertThat(result.getStockAfter()).isZero();
    }

    private static Variant variantWithStock(int stock) {
        Item item = Item.builder().basePrice(BigDecimal.TEN).build();
        return Variant.builder().item(item).sku("SKU-1").name("V").stock(stock).minStock(5).build();
    }

    private static User user() {
        return User.builder().name("Actor").email("actor@logivault.test")
                .passwordHash("hash").role(Role.STAFF).active(true).build();
    }
}
