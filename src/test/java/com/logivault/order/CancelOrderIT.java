package com.logivault.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logivault.entity.Role;
import com.logivault.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CancelOrderIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void cancel_restoresStockAndRecordsSaleCancelMovement() throws Exception {
        String token = testData.loginToken(Role.ADMIN, PASSWORD);
        UUID variant = testData.createVariantWithStock(token, 10);
        String orderId = createOrder(token, variant, 3);

        mockMvc.perform(cancel(orderId, "customer changed mind", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelReason").value("customer changed mind"))
                .andExpect(jsonPath("$.cancelledBy.id").isNotEmpty())
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/variants/" + variant).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.stock").value(10));
        mockMvc.perform(get("/api/v1/variants/" + variant + "/movements?type=SALE_CANCEL")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].qty").value(3))
                .andExpect(jsonPath("$.content[0].stockBefore").value(7))
                .andExpect(jsonPath("$.content[0].stockAfter").value(10))
                .andExpect(jsonPath("$.content[0].reason").value("customer changed mind"));
    }

    @Test
    void cancelTwice_returns409() throws Exception {
        String token = testData.loginToken(Role.ADMIN, PASSWORD);
        String orderId = createOrder(token, testData.createVariantWithStock(token, 5), 1);
        mockMvc.perform(cancel(orderId, "first", token)).andExpect(status().isOk());

        mockMvc.perform(cancel(orderId, "second", token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS"));
    }

    @Test
    void cancelWithoutReason_returns400() throws Exception {
        String token = testData.loginToken(Role.ADMIN, PASSWORD);
        String orderId = createOrder(token, testData.createVariantWithStock(token, 5), 1);

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void cancelUnknownOrder_returns404() throws Exception {
        String token = testData.loginToken(Role.ADMIN, PASSWORD);

        mockMvc.perform(cancel(UUID.randomUUID().toString(), "x", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void cancelWhileVariantInactive_stillRestocks() throws Exception {
        String token = testData.loginToken(Role.ADMIN, PASSWORD);
        UUID variant = testData.createVariantWithStock(token, 5);
        String orderId = createOrder(token, variant, 2);
        mockMvc.perform(delete("/api/v1/variants/" + variant).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(cancel(orderId, "variant retired", token)).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/variants/" + variant).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.stock").value(5));
    }

    @Test
    void staff_canCancel() throws Exception {
        String admin = testData.loginToken(Role.ADMIN, PASSWORD);
        String orderId = createOrder(admin, testData.createVariantWithStock(admin, 5), 1);

        mockMvc.perform(cancel(orderId, "staff cancel", testData.loginToken(Role.STAFF, PASSWORD)))
                .andExpect(status().isOk());
    }

    private String createOrder(String token, UUID variant, int qty) throws Exception {
        String body = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("lines", List.of(Map.of("variantId", variant.toString(), "qty", qty))))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder cancel(String orderId, String reason, String token)
            throws Exception {
        return post("/api/v1/orders/" + orderId + "/cancel")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("reason", reason)));
    }
}
