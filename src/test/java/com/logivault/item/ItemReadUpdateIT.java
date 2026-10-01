package com.logivault.item;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logivault.item.dto.CreateItemRequest;
import com.logivault.item.dto.UpdateItemRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ItemReadUpdateIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void list_filterByQ_isCaseInsensitive() throws Exception {
        String adminToken = adminAccessToken();
        String uniqueName = "Searchable Hoodie " + System.nanoTime();
        createItem(adminToken, new CreateItemRequest(uniqueName, null, BigDecimal.TEN, null));

        mockMvc.perform(get("/api/v1/items").param("q", uniqueName.toUpperCase()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value(uniqueName));
    }

    @Test
    void itemDetail_totalStockEqualsSumOfVariantStock() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode created = createItem(adminToken, new CreateItemRequest("Stock Sum Item", null, BigDecimal.TEN, List.of(
                new CreateVariantRequest("SKU-A-" + System.nanoTime(), "A", null, null, null),
                new CreateVariantRequest("SKU-B-" + System.nanoTime(), "B", null, null, null)
        )));
        UUID itemId = UUID.fromString(created.get("id").asText());
        UUID variantAId = UUID.fromString(created.get("variants").get(0).get("id").asText());
        UUID variantBId = UUID.fromString(created.get("variants").get(1).get("id").asText());

        // Stock-in isn't built until T-13; set stock directly for this aggregation test only.
        jdbcTemplate.update("update variants set stock = ? where id = ?", 7, variantAId);
        jdbcTemplate.update("update variants set stock = ? where id = ?", 3, variantBId);

        mockMvc.perform(get("/api/v1/items").param("q", "Stock Sum Item").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].variantCount").value(2))
                .andExpect(jsonPath("$.content[0].totalStock").value(10));
    }

    @Test
    void updateItem_changesBasePrice_andEffectivePriceOfVariantsWithoutOwnPrice() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode created = createItem(adminToken, new CreateItemRequest("Repriceable Item", null, new BigDecimal("50000.00"), List.of(
                new CreateVariantRequest("SKU-" + System.nanoTime(), "Only variant", null, null, null)
        )));
        UUID itemId = UUID.fromString(created.get("id").asText());

        mockMvc.perform(put("/api/v1/items/" + itemId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateItemRequest("Repriceable Item", "updated", new BigDecimal("70000.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basePrice").value(70000.00))
                .andExpect(jsonPath("$.variants[0].effectivePrice").value(70000.00));
    }

    @Test
    void getById_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/items/" + UUID.randomUUID()).header("Authorization", "Bearer " + adminAccessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ITEM_NOT_FOUND"));
    }

    @Test
    void staff_canReadItems_butCannotUpdateOrAddVariants() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode created = createItem(adminToken, new CreateItemRequest("Staff Read Item", null, BigDecimal.TEN, null));
        UUID itemId = UUID.fromString(created.get("id").asText());

        User staff = testData.createUser(Role.STAFF, PASSWORD);
        String staffToken = testData.accessTokenFor(staff, PASSWORD);

        mockMvc.perform(get("/api/v1/items").header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/items/" + itemId).header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/items/" + itemId + "/variants").header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/items/" + itemId)
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateItemRequest("x", null, BigDecimal.ONE))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/items/" + itemId + "/variants")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateVariantRequest("SKU-" + System.nanoTime(), "V", null, null, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void addVariant_asAdmin_appearsInItemVariantsList() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode created = createItem(adminToken, new CreateItemRequest("Extendable Item", null, BigDecimal.TEN, null));
        UUID itemId = UUID.fromString(created.get("id").asText());
        String newSku = "SKU-NEW-" + System.nanoTime();

        mockMvc.perform(post("/api/v1/items/" + itemId + "/variants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateVariantRequest(newSku, "New Variant", null, null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value(newSku));

        mockMvc.perform(get("/api/v1/items/" + itemId + "/variants").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    private String adminAccessToken() throws Exception {
        User admin = testData.createUser(Role.ADMIN, PASSWORD);
        return testData.accessTokenFor(admin, PASSWORD);
    }

    private JsonNode createItem(String accessToken, CreateItemRequest request) throws Exception {
        String body = mockMvc.perform(postItem(request, accessToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private MockHttpServletRequestBuilder postItem(CreateItemRequest request, String accessToken) throws Exception {
        return post("/api/v1/items")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));
    }
}
