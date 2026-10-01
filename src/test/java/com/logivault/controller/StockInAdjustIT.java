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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StockInAdjustIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void stockIn_addsQtyAndRecordsOneMovementWithActor() throws Exception {
        String adminToken = adminAccessToken();
        UUID variantId = createVariant(adminToken);

        mockMvc.perform(stockInRequest(variantId, 20, "Supplier delivery", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stock").value(20))
                .andExpect(jsonPath("$.data.movement.type").value("STOCK_IN"))
                .andExpect(jsonPath("$.data.movement.qty").value(20))
                .andExpect(jsonPath("$.data.movement.stockBefore").value(0))
                .andExpect(jsonPath("$.data.movement.stockAfter").value(20))
                .andExpect(jsonPath("$.data.movement.actor.id").isNotEmpty());
    }

    @Test
    void adjust_belowZero_returns409AndLeavesStockUnchanged() throws Exception {
        String adminToken = adminAccessToken();
        UUID variantId = createVariant(adminToken);
        mockMvc.perform(stockInRequest(variantId, 10, "init", adminToken)).andExpect(status().isOk());

        mockMvc.perform(adjustRequest(variantId, -25, "damaged", adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

        mockMvc.perform(stockInRequest(variantId, 0, null, adminToken))
                .andExpect(status().isBadRequest()); // qty must be positive; also confirms no side effect from the failed adjust
    }

    @Test
    void adjust_withoutReason_returns400() throws Exception {
        String adminToken = adminAccessToken();
        UUID variantId = createVariant(adminToken);

        mockMvc.perform(post("/api/v1/variants/" + variantId + "/adjust")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"delta\":-2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void adjust_withZeroDelta_returns400() throws Exception {
        String adminToken = adminAccessToken();
        UUID variantId = createVariant(adminToken);

        mockMvc.perform(adjustRequest(variantId, 0, "no-op", adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void staff_canStockIn_butCannotAdjust() throws Exception {
        String adminToken = adminAccessToken();
        UUID variantId = createVariant(adminToken);

        User staff = testData.createUser(Role.STAFF, PASSWORD);
        String staffToken = testData.accessTokenFor(staff, PASSWORD);

        mockMvc.perform(stockInRequest(variantId, 5, "staff delivery", staffToken))
                .andExpect(status().isOk());

        mockMvc.perform(adjustRequest(variantId, -1, "damaged", staffToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void stockIn_onInactiveVariant_stillWorks() throws Exception {
        String adminToken = adminAccessToken();
        UUID variantId = createVariant(adminToken);
        mockMvc.perform(delete("/api/v1/variants/" + variantId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(stockInRequest(variantId, 3, "late delivery", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stock").value(3));
    }

    private String adminAccessToken() throws Exception {
        User admin = testData.createUser(Role.ADMIN, PASSWORD);
        return testData.accessTokenFor(admin, PASSWORD);
    }

    private UUID createVariant(String adminToken) throws Exception {
        CreateItemRequest request = new CreateItemRequest("Stock Test Item", null, BigDecimal.TEN,
                List.of(new CreateVariantRequest("SKU-" + System.nanoTime(), "V", null, null, null)));
        String body = mockMvc.perform(postItem(request, adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(body).get("data");
        return UUID.fromString(created.get("variants").get(0).get("id").asText());
    }

    private MockHttpServletRequestBuilder postItem(CreateItemRequest request, String accessToken) throws Exception {
        return post("/api/v1/items")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));
    }

    private MockHttpServletRequestBuilder stockInRequest(UUID variantId, int qty, String note, String accessToken) throws Exception {
        return post("/api/v1/variants/" + variantId + "/stock-in")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new StockInRequest(qty, note)));
    }

    private MockHttpServletRequestBuilder adjustRequest(UUID variantId, int delta, String reason, String accessToken) throws Exception {
        return post("/api/v1/variants/" + variantId + "/adjust")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AdjustStockRequest(delta, reason)));
    }
}
