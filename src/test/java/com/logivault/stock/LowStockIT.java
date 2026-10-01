package com.logivault.stock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logivault.entity.Role;
import com.logivault.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LowStockIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void includesAtMinStock_excludesAbove_mostCriticalFirst() throws Exception {
        String admin = testData.loginToken(Role.ADMIN, PASSWORD);
        String tag = "LS" + System.nanoTime();
        UUID atMin = variant(admin, tag + "-A", 5, 5);       // gap 0
        UUID above = variant(admin, tag + "-B", 5, 6);       // excluded
        UUID empty = variant(admin, tag + "-C", 5, 0);       // gap -5, most critical
        UUID below = variant(admin, tag + "-D", 5, 2);       // gap -3

        List<String> ids = lowStockIds(admin);

        assertThat(ids).contains(atMin.toString(), below.toString(), empty.toString());
        assertThat(ids).doesNotContain(above.toString());
        assertThat(ids.indexOf(empty.toString())).isLessThan(ids.indexOf(below.toString()));
        assertThat(ids.indexOf(below.toString())).isLessThan(ids.indexOf(atMin.toString()));
    }

    @Test
    void excludesInactiveVariantsAndVariantsOfInactiveItems() throws Exception {
        String admin = testData.loginToken(Role.ADMIN, PASSWORD);
        String tag = "LS" + System.nanoTime();
        UUID inactiveVariant = variant(admin, tag + "-A", 5, 1);
        mockMvc.perform(delete("/api/v1/variants/" + inactiveVariant).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());

        UUID inactiveItemVariant = variant(admin, tag + "-B", 5, 1);
        String itemId = objectMapper.readTree(mockMvc.perform(get("/api/v1/variants/" + inactiveItemVariant)
                        .header("Authorization", "Bearer " + admin)).andReturn().getResponse().getContentAsString())
                .get("itemId").asText();
        mockMvc.perform(delete("/api/v1/items/" + itemId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());

        List<String> ids = lowStockIds(admin);

        assertThat(ids).doesNotContain(inactiveVariant.toString(), inactiveItemVariant.toString());
    }

    @Test
    void staffCanRead_andNoTokenIs401() throws Exception {
        String staff = testData.loginToken(Role.STAFF, PASSWORD);

        mockMvc.perform(get("/api/v1/stock/low").header("Authorization", "Bearer " + staff)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/stock/low")).andExpect(status().isUnauthorized());
    }

    // Walks every page so the assertions don't depend on how many low-stock rows other tests left behind.
    private List<String> lowStockIds(String token) throws Exception {
        List<String> ids = new ArrayList<>();
        for (int page = 0; ; page++) {
            JsonNode body = objectMapper.readTree(mockMvc.perform(get("/api/v1/stock/low?size=100&page=" + page)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString());
            body.get("content").forEach(row -> ids.add(row.get("variantId").asText()));
            if (page + 1 >= body.get("totalPages").asInt()) {
                return ids;
            }
        }
    }

    private UUID variant(String admin, String sku, int minStock, int stock) throws Exception {
        Map<String, Object> item = Map.of("name", "Low Stock Item", "basePrice", 10,
                "variants", List.of(Map.of("sku", sku, "name", "V", "minStock", minStock)));
        JsonNode created = objectMapper.readTree(mockMvc.perform(post("/api/v1/items")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        UUID id = UUID.fromString(created.get("variants").get(0).get("id").asText());
        if (stock > 0) {
            testData.stockIn(admin, id, stock);
        }
        return id;
    }
}
