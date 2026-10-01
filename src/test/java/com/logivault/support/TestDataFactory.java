package com.logivault.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logivault.user.Role;
import com.logivault.user.User;
import com.logivault.user.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;

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
        return objectMapper.readTree(body);
    }

    public String accessTokenFor(User user, String rawPassword) throws Exception {
        return loginAs(user.getEmail(), rawPassword).get("accessToken").asText();
    }

    private record LoginBody(String email, String password) {
    }
}
