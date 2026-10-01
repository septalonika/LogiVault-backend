package com.logivault.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logivault.dto.item.CreateItemRequest;
import com.logivault.dto.variant.CreateVariantRequest;
import com.logivault.dto.variant.UpdateVariantRequest;
import com.logivault.entity.Role;
import com.logivault.entity.User;
import com.logivault.entity.Variant;
import com.logivault.repository.VariantRepository;
import com.logivault.stock.MovementType;
import com.logivault.stock.StockService;
import com.logivault.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VariantIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VariantRepository variantRepository;

    @Autowired
    private StockService stockService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void getBySku_worksWithLowercaseInput() throws Exception {
        String adminToken = adminAccessToken();
        String sku = "SKU-" + System.nanoTime();
        createItemWithVariant(adminToken, sku);

        mockMvc.perform(get("/api/v1/variants/sku/" + sku.toLowerCase()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value(sku))
                .andExpect(jsonPath("$.itemName").value("Variant Test Item"))
                .andExpect(jsonPath("$.itemActive").value(true));
    }

    @Test
    void getBySku_unknown_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/variants/sku/does-not-exist").header("Authorization", "Bearer " + adminAccessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VARIANT_NOT_FOUND"));
    }

    @Test
    void deactivateThenReactivate_keepsTheRowAndFlipsActive() throws Exception {
        String adminToken = adminAccessToken();
        UUID variantId = createItemWithVariant(adminToken, "SKU-" + System.nanoTime());

        mockMvc.perform(delete("/api/v1/variants/" + variantId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/variants/" + variantId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch("/api/v1/variants/" + variantId + "/activate").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void staff_isForbiddenOnUpdateDeactivateAndActivate() throws Exception {
        String adminToken = adminAccessToken();
        UUID variantId = createItemWithVariant(adminToken, "SKU-" + System.nanoTime());

        User staff = testData.createUser(Role.STAFF, PASSWORD);
        String staffToken = testData.accessTokenFor(staff, PASSWORD);

        mockMvc.perform(put("/api/v1/variants/" + variantId)
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateVariantRequest("SKU-X-" + System.nanoTime(), "X", null, null, null))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/variants/" + variantId).header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/variants/" + variantId + "/activate").header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_changesNameAndPrice() throws Exception {
        String adminToken = adminAccessToken();
        String sku = "SKU-" + System.nanoTime();
        UUID variantId = createItemWithVariant(adminToken, sku);

        mockMvc.perform(put("/api/v1/variants/" + variantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateVariantRequest(sku, "Renamed", null, new BigDecimal("12345.00"), 7))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.effectivePrice").value(12345.00))
                .andExpect(jsonPath("$.minStock").value(7));
    }

    @Test
    void update_changingSkuAfterAStockMovementExists_returns409SkuImmutable() throws Exception {
        String adminToken = adminAccessToken();
        String sku = "SKU-" + System.nanoTime();
        UUID variantId = createItemWithVariant(adminToken, sku);
        User actor = testData.createUser(Role.ADMIN, PASSWORD);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Variant locked = variantRepository.findAllByIdForUpdate(List.of(variantId)).get(0);
            stockService.applyMovement(locked, MovementType.STOCK_IN, 5, "initial stock", null, actor);
        });

        mockMvc.perform(put("/api/v1/variants/" + variantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateVariantRequest("SKU-CHANGED-" + System.nanoTime(), "V", null, null, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SKU_IMMUTABLE"));
    }

    private String adminAccessToken() throws Exception {
        User admin = testData.createUser(Role.ADMIN, PASSWORD);
        return testData.accessTokenFor(admin, PASSWORD);
    }

    private UUID createItemWithVariant(String adminToken, String sku) throws Exception {
        CreateItemRequest request = new CreateItemRequest("Variant Test Item", null, BigDecimal.TEN,
                List.of(new CreateVariantRequest(sku, "V", null, null, null)));
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
}
