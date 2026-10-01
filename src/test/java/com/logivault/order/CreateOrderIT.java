package com.logivault.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logivault.item.dto.CreateItemRequest;
import com.logivault.order.dto.CreateOrderRequest;
import com.logivault.order.dto.OrderLineRequest;
import com.logivault.stock.dto.StockInRequest;
import com.logivault.support.AbstractIntegrationTest;
import com.logivault.user.Role;
import com.logivault.user.User;
import com.logivault.variant.dto.CreateVariantRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CreateOrderIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void createOrder_deductsStockAndWritesOneSaleMovement() throws Exception {
        String token = adminToken();
        UUID variantId = createVariant(token, BigDecimal.valueOf(25));
        stockIn(variantId, 10, token);

        mockMvc.perform(postOrder(token, new OrderLineRequest(variantId, 3)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.startsWith("ORD-")))
                .andExpect(jsonPath("$.total").value(75.0))
                .andExpect(jsonPath("$.lines[0].qty").value(3))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(25.0))
                .andExpect(jsonPath("$.createdBy.id").isNotEmpty());

        mockMvc.perform(get("/api/v1/variants/" + variantId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.stock").value(7));

        mockMvc.perform(get("/api/v1/variants/" + variantId + "/movements?type=SALE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].qty").value(-3))
                .andExpect(jsonPath("$.content[0].stockBefore").value(10))
                .andExpect(jsonPath("$.content[0].stockAfter").value(7))
                .andExpect(jsonPath("$.content[0].orderCode").isNotEmpty());
    }

    @Test
    void priceSnapshot_staysUnchangedAfterLaterPriceUpdate() throws Exception {
        String token = adminToken();
        JsonNode item = createItem(token, BigDecimal.valueOf(25));
        UUID variantId = UUID.fromString(item.get("variants").get(0).get("id").asText());
        stockIn(variantId, 10, token);

        String body = mockMvc.perform(postOrder(token, new OrderLineRequest(variantId, 2)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String orderId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(put("/api/v1/items/" + item.get("id").asText())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Order Test Item\",\"basePrice\":99}"))
                .andExpect(status().isOk());

        // The detail endpoint arrives in T-20, so check the snapshot straight from the table.
        BigDecimal unitPrice = jdbc.queryForObject(
                "select unit_price from order_items where order_id = ?::uuid", BigDecimal.class, orderId);
        org.assertj.core.api.Assertions.assertThat(unitPrice).isEqualByComparingTo("25");
    }

    @Test
    void inactiveVariant_returns422() throws Exception {
        String token = adminToken();
        UUID variantId = createVariant(token, BigDecimal.TEN);
        stockIn(variantId, 5, token);
        mockMvc.perform(delete("/api/v1/variants/" + variantId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(postOrder(token, new OrderLineRequest(variantId, 1)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VARIANT_INACTIVE"))
                .andExpect(jsonPath("$.skus.length()").value(1));
    }

    @Test
    void insufficientStock_listsOnlyShortLinesAndChangesNothing() throws Exception {
        String token = adminToken();
        UUID a = createVariant(token, BigDecimal.TEN);
        UUID b = createVariant(token, BigDecimal.TEN);
        stockIn(a, 5, token);
        stockIn(b, 1, token);

        mockMvc.perform(postOrder(token, new OrderLineRequest(a, 2), new OrderLineRequest(b, 3)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.lines.length()").value(1))
                .andExpect(jsonPath("$.lines[0].variantId").value(b.toString()))
                .andExpect(jsonPath("$.lines[0].requested").value(3))
                .andExpect(jsonPath("$.lines[0].available").value(1));

        mockMvc.perform(get("/api/v1/variants/" + a).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.stock").value(5));
    }

    @Test
    void unknownVariant_returns404() throws Exception {
        mockMvc.perform(postOrder(adminToken(), new OrderLineRequest(UUID.randomUUID(), 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VARIANT_NOT_FOUND"));
    }

    @Test
    void duplicateVariantLines_returns400() throws Exception {
        String token = adminToken();
        UUID variantId = createVariant(token, BigDecimal.TEN);
        stockIn(variantId, 10, token);

        mockMvc.perform(postOrder(token, new OrderLineRequest(variantId, 1), new OrderLineRequest(variantId, 2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void emptyLines_returns400() throws Exception {
        mockMvc.perform(postOrder(adminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void zeroQty_returns400() throws Exception {
        String token = adminToken();
        UUID variantId = createVariant(token, BigDecimal.TEN);

        mockMvc.perform(postOrder(token, new OrderLineRequest(variantId, 0)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void staff_canCreateOrder() throws Exception {
        String adminToken = adminToken();
        UUID variantId = createVariant(adminToken, BigDecimal.TEN);
        stockIn(variantId, 4, adminToken);
        User staff = testData.createUser(Role.STAFF, PASSWORD);

        mockMvc.perform(postOrder(testData.accessTokenFor(staff, PASSWORD), new OrderLineRequest(variantId, 1)))
                .andExpect(status().isCreated());
    }

    @Test
    void noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private String adminToken() throws Exception {
        User admin = testData.createUser(Role.ADMIN, PASSWORD);
        return testData.accessTokenFor(admin, PASSWORD);
    }

    private JsonNode createItem(String token, BigDecimal basePrice) throws Exception {
        CreateItemRequest request = new CreateItemRequest("Order Test Item", null, basePrice,
                List.of(new CreateVariantRequest("SKU-" + System.nanoTime(), "V", null, null, null)));
        String body = mockMvc.perform(post("/api/v1/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private UUID createVariant(String token, BigDecimal basePrice) throws Exception {
        return UUID.fromString(createItem(token, basePrice).get("variants").get(0).get("id").asText());
    }

    private void stockIn(UUID variantId, int qty, String token) throws Exception {
        mockMvc.perform(post("/api/v1/variants/" + variantId + "/stock-in")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StockInRequest(qty, "seed"))))
                .andExpect(status().isOk());
    }

    private MockHttpServletRequestBuilder postOrder(String token, OrderLineRequest... lines) throws Exception {
        return post("/api/v1/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateOrderRequest(List.of(lines), null)));
    }
}
