package com.logivault.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logivault.entity.Role;
import com.logivault.entity.User;
import com.logivault.repository.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
public class TestDataFactory {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public TestDataFactory(UserRepository userRepository, PasswordEncoder passwordEncoder,
                            MockMvc mockMvc, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    public String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@logivault.test";
    }

    public User createUser(Role role, String rawPassword) {
        return userRepository.save(User.builder()
                .name("Test " + role.name())
                .email(uniqueEmail())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .active(true)
                .build());
    }

    public JsonNode loginAs(String email, String rawPassword) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginBody(email, rawPassword))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("data");
    }

    public String accessTokenFor(User user, String rawPassword) throws Exception {
        return loginAs(user.getEmail(), rawPassword).get("accessToken").asText();
    }

    public String loginToken(Role role, String rawPassword) throws Exception {
        return accessTokenFor(createUser(role, rawPassword), rawPassword);
    }

    // Creates an item with one variant and stocks it in, all through the API.
    public UUID createVariantWithStock(String adminToken, int stock) throws Exception {
        Map<String, Object> item = Map.of("name", "Fixture Item", "basePrice", BigDecimal.TEN,
                "variants", List.of(Map.of("sku", "SKU-" + UUID.randomUUID(), "name", "V")));
        String body = mockMvc.perform(post("/api/v1/items")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID variantId = UUID.fromString(objectMapper.readTree(body).get("data").get("variants").get(0).get("id").asText());
        if (stock > 0) {
            stockIn(adminToken, variantId, stock);
        }
        return variantId;
    }

    public void stockIn(String token, UUID variantId, int qty) throws Exception {
        mockMvc.perform(post("/api/v1/variants/" + variantId + "/stock-in")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("qty", qty, "note", "fixture"))))
                .andExpect(status().isOk());
    }

    private record LoginBody(String email, String password) {
    }
}
