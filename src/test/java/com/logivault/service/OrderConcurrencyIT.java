package com.logivault.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logivault.entity.Role;
import com.logivault.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderConcurrencyIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void lastUnitRace_exactlyOneOrderWins() throws Exception {
        String admin = testData.loginToken(Role.ADMIN, PASSWORD);
        UUID variant = testData.createVariantWithStock(admin, 1);
        int threads = 20;
        List<String> tokens = tokens(threads);

        List<Integer> statuses = runConcurrently(threads, i -> () -> placeOrder(tokens.get(i), List.of(line(variant, 1))));

        assertThat(statuses.stream().filter(s -> s == 201)).hasSize(1);
        assertThat(statuses.stream().filter(s -> s == 409)).hasSize(threads - 1);
        assertThat(stockOf(variant)).isZero();
        assertThat(movementCount(variant, "SALE")).isEqualTo(1);
    }

    @Test
    void allOrNothing_shortLineRollsBackEverything() throws Exception {
        String admin = testData.loginToken(Role.ADMIN, PASSWORD);
        UUID a = testData.createVariantWithStock(admin, 5);
        UUID b = testData.createVariantWithStock(admin, 1);
        Integer counterBefore = counterToday();
        long ordersBefore = jdbc.queryForObject("select count(*) from orders", Long.class);
        long movementsBefore = movementCount(a, null) + movementCount(b, null);

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("lines", List.of(line(a, 2), line(b, 3))))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.lines.length()").value(1))
                .andExpect(jsonPath("$.lines[0].variantId").value(b.toString()))
                .andExpect(jsonPath("$.lines[0].requested").value(3))
                .andExpect(jsonPath("$.lines[0].available").value(1));

        assertThat(stockOf(a)).isEqualTo(5);
        assertThat(stockOf(b)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from orders", Long.class)).isEqualTo(ordersBefore);
        assertThat(movementCount(a, null) + movementCount(b, null)).isEqualTo(movementsBefore);
        assertThat(counterToday()).isEqualTo(counterBefore);
    }

    @Test
    void oppositeLineOrders_doNotDeadlock() throws Exception {
        String admin = testData.loginToken(Role.ADMIN, PASSWORD);
        UUID a = testData.createVariantWithStock(admin, 100);
        UUID b = testData.createVariantWithStock(admin, 100);
        int threads = 10;
        List<String> tokens = tokens(threads);

        List<Integer> statuses = runConcurrently(threads, i -> () -> placeOrder(tokens.get(i),
                i % 2 == 0 ? List.of(line(a, 1), line(b, 1)) : List.of(line(b, 1), line(a, 1))));

        assertThat(statuses).allMatch(s -> s == 201);
        assertThat(stockOf(a)).isEqualTo(100 - threads);
        assertThat(stockOf(b)).isEqualTo(100 - threads);
        assertThat(movementCount(a, "SALE")).isEqualTo(threads);
        assertThat(movementCount(b, "SALE")).isEqualTo(threads);
    }

    private List<String> tokens(int n) throws Exception {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            tokens.add(testData.loginToken(Role.STAFF, PASSWORD));
        }
        return tokens;
    }

    private Map<String, Object> line(UUID variantId, int qty) {
        return Map.of("variantId", variantId.toString(), "qty", qty);
    }

    private int placeOrder(String token, List<Map<String, Object>> lines) throws Exception {
        return mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("lines", lines))))
                .andReturn().getResponse().getStatus();
    }

    // All tasks wait on a shared latch so the requests hit the database at the same moment.
    private List<Integer> runConcurrently(int threads, java.util.function.IntFunction<Callable<Integer>> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                Callable<Integer> call = task.apply(i);
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    return call.call();
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            List<Integer> results = new ArrayList<>();
            for (Future<Integer> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private int stockOf(UUID variantId) {
        return jdbc.queryForObject("select stock from variants where id = ?", Integer.class, variantId);
    }

    private long movementCount(UUID variantId, String type) {
        return type == null
                ? jdbc.queryForObject("select count(*) from stock_movements where variant_id = ?", Long.class, variantId)
                : jdbc.queryForObject("select count(*) from stock_movements where variant_id = ? and type = ?",
                        Long.class, variantId, type);
    }

    private Integer counterToday() {
        List<Integer> rows = jdbc.queryForList("select last_seq from order_code_counters order by day desc limit 1", Integer.class);
        return rows.isEmpty() ? null : rows.get(0);
    }
}
