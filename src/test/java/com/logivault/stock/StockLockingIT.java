package com.logivault.stock;

import com.logivault.item.Item;
import com.logivault.item.ItemRepository;
import com.logivault.support.AbstractIntegrationTest;
import com.logivault.user.Role;
import com.logivault.user.User;
import com.logivault.variant.Variant;
import com.logivault.variant.VariantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockLockingIT extends AbstractIntegrationTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private VariantRepository variantRepository;

    @Autowired
    private StockService stockService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void applyMovement_outsideATransaction_throwsIllegalTransactionState() {
        assertThatThrownBy(() -> stockService.applyMovement(variantWithStock(1), MovementType.STOCK_IN, 1, null, null, admin()))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void concurrentStockIn_onTheSameVariant_appliesBothDeltasSerially() throws Exception {
        UUID variantId = new TransactionTemplate(transactionManager)
                .execute(status -> variantWithStock(0).getId());
        User actor = testData.createUser(Role.ADMIN, "Password123!");

        int threads = 10;
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        Variant locked = variantRepository.findAllByIdForUpdate(List.of(variantId)).get(0);
                        stockService.applyMovement(locked, MovementType.STOCK_IN, 1, null, null, actor);
                    });
                } catch (Exception e) {
                    failures.incrementAndGet();
                }
            });
        }
        startGate.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(failures.get()).isZero();
        Variant reloaded = variantRepository.findById(variantId).orElseThrow();
        assertThat(reloaded.getStock()).isEqualTo(threads);
    }

    private Variant variantWithStock(int stock) {
        Item item = itemRepository.save(Item.builder().name("Lock Test Item").basePrice(BigDecimal.TEN).build());
        return variantRepository.saveAndFlush(Variant.builder()
                .item(item).sku("LOCK-" + System.nanoTime()).name("V").stock(stock).minStock(5).build());
    }

    private User admin() {
        return testData.createUser(Role.ADMIN, "Password123!");
    }
}
