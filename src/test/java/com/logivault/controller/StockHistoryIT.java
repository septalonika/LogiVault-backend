package com.logivault.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logivault.dto.item.CreateItemRequest;
import com.logivault.dto.stock.AdjustStockRequest;
import com.logivault.dto.stock.StockInRequest;
import com.logivault.dto.variant.CreateVariantRequest;
import com.logivault.entity.Role;
import com.logivault.entity.User;
import com.logivault.support.AbstractIntegrationTest;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StockHistoryIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void history_showsNewestFirstWithCorrectBeforeAfterAndActor() throws Exception {
        User admin = testData.createUser(Role.ADMIN, PASSWORD);
        String adminToken = testData.accessTokenFor(admin, PASSWORD);
        UUID variantId = createVariant(adminToken);

        mockMvc.perform(stockIn(variantId, 10, "init", adminToken)).andExpect(status().isOk());
        mockMvc.perform(adjust(variantId, -3, "damaged", adminToken)).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/variants/" + variantId + "/movements").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].type").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.content[0].stockBefore").value(10))
                .andExpect(jsonPath("$.content[0].stockAfter").value(7))
                .andExpect(jsonPath("$.content[0].actor.name").value(admin.getName()))
                .andExpect(jsonPath("$.content[1].type").value("STOCK_IN"));
    }

    @Test
    void history_filterByType_onlyReturnsThatType() throws Exception {
        String adminToken = adminAccessToken();
        UUID variantId = createVariant(adminToken);
        mockMvc.perform(stockIn(variantId, 10, "init", adminToken)).andExpect(status().isOk());
        mockMvc.perform(adjust(variantId, -2, "damaged", adminToken)).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/variants/" + variantId + "/movements")
                        .param("type", "ADJUSTMENT")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].type").value("ADJUSTMENT"));
    }

    @Test
    void history_dateFilter_isInclusiveInBusinessZone() throws Exception {
        String adminToken = adminAccessToken();
        UUID variantId = createVariant(adminToken);
        mockMvc.perform(stockIn(variantId, 1, "late-night delivery", adminToken)).andExpect(status().isOk());

        ZonedDateTime nowInJakarta = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"));
        String today = nowInJakarta.toLocalDate().toString();

        mockMvc.perform(get("/api/v1/variants/" + variantId + "/movements")
                        .param("from", today).param("to", today)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void history_fromAfterTo_returns400() throws Exception {
        String adminToken = adminAccessToken();
        UUID variantId = createVariant(adminToken);

        mockMvc.perform(get("/api/v1/variants/" + variantId + "/movements")
                        .param("from", "2026-02-01").param("to", "2026-01-01")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void history_unknownVariant_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/variants/" + UUID.randomUUID() + "/movements")
                        .header("Authorization", "Bearer " + adminAccessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VARIANT_NOT_FOUND"));
    }

    @Test
    void history_doesNotCauseNPlusOneQueries() throws Exception {
        String adminToken = adminAccessToken();
        UUID variantId = createVariant(adminToken);
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(stockIn(variantId, 1, "delivery " + i, adminToken)).andExpect(status().isOk());
        }

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        mockMvc.perform(get("/api/v1/variants/" + variantId + "/movements").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5));

        // 1 query for the variant existence check, 1 for the count, 1 for the page with actor fetch-joined.
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    private String adminAccessToken() throws Exception {
        User admin = testData.createUser(Role.ADMIN, PASSWORD);
        return testData.accessTokenFor(admin, PASSWORD);
    }

    private UUID createVariant(String adminToken) throws Exception {
        CreateItemRequest request = new CreateItemRequest("History Test Item", null, BigDecimal.TEN,
                List.of(new CreateVariantRequest("SKU-" + System.nanoTime(), "V", null, null, null)));
        String body = mockMvc.perform(postItem(request, adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(body);
        return UUID.fromString(created.get("variants").get(0).get("id").asText());
    }

    private MockHttpServletRequestBuilder postItem(CreateItemRequest request, String accessToken) throws Exception {
        return post("/api/v1/items")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));
    }

    private MockHttpServletRequestBuilder stockIn(UUID variantId, int qty, String note, String accessToken) throws Exception {
        return post("/api/v1/variants/" + variantId + "/stock-in")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new StockInRequest(qty, note)));
    }

    private MockHttpServletRequestBuilder adjust(UUID variantId, int delta, String reason, String accessToken) throws Exception {
        return post("/api/v1/variants/" + variantId + "/adjust")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AdjustStockRequest(delta, reason)));
    }
}
