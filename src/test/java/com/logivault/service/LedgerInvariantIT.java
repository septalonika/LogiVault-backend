package com.logivault.service;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class LedgerInvariantIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void randomOperations_keepLedgerEqualToStock() throws Exception {
        String admin = testData.loginToken(Role.ADMIN, PASSWORD);
        List<UUID> variants = List.of(
                testData.createVariantWithStock(admin, 0),
                testData.createVariantWithStock(admin, 0),
                testData.createVariantWithStock(admin, 0));
        Random random = new Random(42);
        List<String> orderIds = new ArrayList<>();

        for (int i = 0; i < 150; i++) {
            UUID variant = variants.get(random.nextInt(variants.size()));
            switch (random.nextInt(4)) {
                case 0 -> send(admin, "/api/v1/variants/" + variant + "/stock-in",
                        Map.of("qty", 1 + random.nextInt(20), "note", "random"));
                case 1 -> {
                    int delta = random.nextInt(21) - 10;
                    send(admin, "/api/v1/variants/" + variant + "/adjust",
                            Map.of("delta", delta == 0 ? 1 : delta, "reason", "random"));
                }
                case 2 -> {
                    String body = send(admin, "/api/v1/orders",
                            Map.of("lines", List.of(Map.of("variantId", variant.toString(), "qty", 1 + random.nextInt(8)))));
                    JsonNode created = objectMapper.readTree(body);
                    if (created.path("data").has("id")) {
                        orderIds.add(created.get("data").get("id").asText());
                    }
                }
                default -> {
                    if (!orderIds.isEmpty()) {
                        send(admin, "/api/v1/orders/" + orderIds.remove(random.nextInt(orderIds.size())) + "/cancel",
                                Map.of("reason", "random"));
                    }
                }
            }
        }

        for (UUID variant : variants) {
            Long sum = jdbc.queryForObject(
                    "select coalesce(sum(qty), 0) from stock_movements where variant_id = ?", Long.class, variant);
            Integer stock = jdbc.queryForObject("select stock from variants where id = ?", Integer.class, variant);
            assertThat(sum).isEqualTo(stock.longValue());

            Long broken = jdbc.queryForObject(
                    "select count(*) from stock_movements where variant_id = ? and stock_after <> stock_before + qty",
                    Long.class, variant);
            assertThat(broken).isZero();
        }
    }

    // Rejected requests (e.g. 409 on an overdraw) are expected in a random run, so statuses are not asserted.
    private String send(String token, String path, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post(path)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn().getResponse().getContentAsString();
    }
}
