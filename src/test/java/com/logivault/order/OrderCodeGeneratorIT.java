package com.logivault.order;

import com.logivault.common.config.LogiVaultProperties;
import com.logivault.support.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class OrderCodeGeneratorIT extends AbstractIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private LogiVaultProperties properties;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void next_onTheSameDay_incrementsSequentially() {
        OrderCodeGenerator generator = generatorAt("2031-10-01T05:00:00Z"); // 12:00 WIB

        String first = inTransaction(generator::next);
        String second = inTransaction(generator::next);

        assertThat(first).isEqualTo("ORD-20311001-0001");
        assertThat(second).isEqualTo("ORD-20311001-0002");
    }

    @Test
    void next_usesBusinessZoneNotUtcDate() {
        // 2031-10-01T18:00:00Z is 2031-10-02T01:00 in Asia/Jakarta (UTC+7).
        OrderCodeGenerator generator = generatorAt("2031-10-01T18:00:00Z");

        String code = inTransaction(generator::next);

        assertThat(code).startsWith("ORD-20311002-");
    }

    @Test
    void next_concurrentCalls_produceUniqueCodes() throws Exception {
        OrderCodeGenerator generator = generatorAt("2031-11-05T05:00:00Z");
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> inTransaction(generator::next)));
        }
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        Set<String> codes = new HashSet<>();
        for (Future<String> future : futures) {
            codes.add(future.get());
        }
        assertThat(codes).hasSize(threads);
    }

    private OrderCodeGenerator generatorAt(String instant) {
        Clock fixedClock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
        return new OrderCodeGenerator(entityManager, fixedClock, properties);
    }

    private String inTransaction(Supplier<String> action) {
        return new TransactionTemplate(transactionManager).execute(status -> action.get());
    }
}
