package com.logivault.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logivault.support.AbstractIntegrationTest;
import com.logivault.user.Role;
import com.logivault.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderQueryIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void filterByCreator_andStaffSeesOthersOrders() throws Exception {
        String admin = testData.loginToken(Role.ADMIN, PASSWORD);
        User staffUser = testData.createUser(Role.STAFF, PASSWORD);
        String staff = testData.accessTokenFor(staffUser, PASSWORD);
        UUID variant = testData.createVariantWithStock(admin, 20);
        String adminOrder = createOrder(admin, List.of(line(variant, 1)));
        createOrder(staff, List.of(line(variant, 2)));

        // STAFF can read an order created by the ADMIN: reads are not restricted per user
        mockMvc.perform(get("/api/v1/orders/" + adminOrder).header("Authorization", "Bearer " + staff))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/orders?createdBy=" + staffUser.getId()).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].createdBy.id").value(staffUser.getId().toString()))
                .andExpect(jsonPath("$.content[0].createdBy.name").isNotEmpty());
    }

    @Test
    void filterByStatus_andItemCount() throws Exception {
        String admin = testData.loginToken(Role.ADMIN, PASSWORD);
        UUID a = testData.createVariantWithStock(admin, 20);
        UUID b = testData.createVariantWithStock(admin, 20);
        String twoLines = createOrder(admin, List.of(line(a, 1), line(b, 1)));
        String cancelled = createOrder(admin, List.of(line(a, 1)));
        mockMvc.perform(post("/api/v1/orders/" + cancelled + "/cancel")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"test\"}"))
                .andExpect(status().isOk());

        UUID me = UUID.fromString(jdbc.queryForObject("select created_by::text from orders where id = ?::uuid", String.class, twoLines));
        String base = "/api/v1/orders?createdBy=" + me;

        mockMvc.perform(get(base + "&status=CANCELLED").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(cancelled))
                .andExpect(jsonPath("$.content[0].itemCount").value(1));

        mockMvc.perform(get(base + "&status=COMPLETED").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(twoLines))
                .andExpect(jsonPath("$.content[0].itemCount").value(2));
    }

    @Test
    void filterByDateRange_isInclusiveInBusinessZone() throws Exception {
        String admin = testData.loginToken(Role.ADMIN, PASSWORD);
        UUID variant = testData.createVariantWithStock(admin, 5);
        String orderId = createOrder(admin, List.of(line(variant, 1)));
        UUID me = UUID.fromString(jdbc.queryForObject("select created_by::text from orders where id = ?::uuid", String.class, orderId));
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Jakarta"));

        mockMvc.perform(get("/api/v1/orders?createdBy=" + me + "&from=" + today + "&to=" + today)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/orders?createdBy=" + me + "&to=" + today.minusDays(1))
                        .header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/orders?from=" + today.plusDays(1) + "&to=" + today)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void detail_showsCancelInfoForCancelledOrder() throws Exception {
        String admin = testData.loginToken(Role.ADMIN, PASSWORD);
        UUID variant = testData.createVariantWithStock(admin, 5);
        String orderId = createOrder(admin, List.of(line(variant, 2)));
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"wrong item\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/orders/" + orderId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelReason").value("wrong item"))
                .andExpect(jsonPath("$.cancelledBy.id").isNotEmpty())
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty())
                .andExpect(jsonPath("$.lines[0].sku").isNotEmpty())
                .andExpect(jsonPath("$.lines[0].itemName").value("Fixture Item"));
    }

    @Test
    void detail_unknownOrder_returns404() throws Exception {
        String admin = testData.loginToken(Role.ADMIN, PASSWORD);

        mockMvc.perform(get("/api/v1/orders/" + UUID.randomUUID()).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/orders")).andExpect(status().isUnauthorized());
    }

    private Map<String, Object> line(UUID variantId, int qty) {
        return Map.of("variantId", variantId.toString(), "qty", qty);
    }

    private String createOrder(String token, List<Map<String, Object>> lines) throws Exception {
        String body = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("lines", lines))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(body);
        return created.get("id").asText();
    }
}
