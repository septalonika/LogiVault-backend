package com.logivault.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logivault.entity.Role;
import com.logivault.entity.User;
import com.logivault.item.dto.CreateItemRequest;
import com.logivault.support.AbstractIntegrationTest;
import com.logivault.variant.dto.CreateVariantRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CreateItemIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void create_withTwoVariants_returns201AndEachStartsAtZeroStock() throws Exception {
        String adminToken = adminAccessToken();
        CreateItemRequest request = new CreateItemRequest("Basic T-Shirt", "Cotton 30s", new BigDecimal("85000.00"),
                List.of(
                        new CreateVariantRequest(uniqueSku("TS-RED-M"), "Red / M", Map.of("color", "Red", "size", "M"), null, null),
                        new CreateVariantRequest(uniqueSku("TS-RED-XL"), "Red / XL", Map.of("color", "Red", "size", "XL"), new BigDecimal("95000.00"), 10)
                ));

        mockMvc.perform(postItem(request, adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.variants.length()").value(2))
                .andExpect(jsonPath("$.variants[0].stock").value(0))
                .andExpect(jsonPath("$.variants[1].stock").value(0))
                .andExpect(jsonPath("$.variants[1].effectivePrice").value(95000.00))
                .andExpect(jsonPath("$.variants[0].effectivePrice").value(85000.00));
    }

    @Test
    void create_withoutVariants_createsOneDefaultVariant() throws Exception {
        CreateItemRequest request = new CreateItemRequest("Plain Mug", null, new BigDecimal("25000.00"), null);

        mockMvc.perform(postItem(request, adminAccessToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.variants.length()").value(1))
                .andExpect(jsonPath("$.variants[0].name").value("Default"))
                .andExpect(jsonPath("$.variants[0].sku").value(startsWith("ITEM-")));
    }

    @Test
    void create_withSkuDuplicatedWithinTheRequest_returns409() throws Exception {
        String sku = uniqueSku("DUP");
        CreateItemRequest request = new CreateItemRequest("Item", null, BigDecimal.TEN, List.of(
                new CreateVariantRequest(sku, "A", null, null, null),
                new CreateVariantRequest(sku.toLowerCase(), "B", null, null, null)
        ));

        mockMvc.perform(postItem(request, adminAccessToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SKU_ALREADY_EXISTS"));
    }

    @Test
    void create_withSkuThatAlreadyExistsInTheDatabase_returns409() throws Exception {
        String adminToken = adminAccessToken();
        String sku = uniqueSku("EXIST");
        mockMvc.perform(postItem(new CreateItemRequest("First", null, BigDecimal.TEN,
                        List.of(new CreateVariantRequest(sku, "A", null, null, null))), adminToken))
                .andExpect(status().isCreated());

        mockMvc.perform(postItem(new CreateItemRequest("Second", null, BigDecimal.TEN,
                        List.of(new CreateVariantRequest(sku.toLowerCase(), "B", null, null, null))), adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SKU_ALREADY_EXISTS"));
    }

    @Test
    void create_withNegativePrice_returns400() throws Exception {
        CreateItemRequest request = new CreateItemRequest("Item", null, new BigDecimal("-1.00"), null);

        mockMvc.perform(postItem(request, adminAccessToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void create_asStaff_returns403() throws Exception {
        User staff = testData.createUser(Role.STAFF, PASSWORD);
        String staffToken = testData.accessTokenFor(staff, PASSWORD);
        CreateItemRequest request = new CreateItemRequest("Item", null, BigDecimal.TEN, null);

        mockMvc.perform(postItem(request, staffToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private String adminAccessToken() throws Exception {
        User admin = testData.createUser(Role.ADMIN, PASSWORD);
        return testData.accessTokenFor(admin, PASSWORD);
    }

    private MockHttpServletRequestBuilder postItem(CreateItemRequest request, String accessToken) throws Exception {
        return post("/api/v1/items")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));
    }

    private static String uniqueSku(String prefix) {
        return prefix + "-" + System.nanoTime();
    }
}
