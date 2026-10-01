package com.logivault.common.security;

import com.logivault.support.AbstractIntegrationTest;
import com.logivault.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityConfigIT extends AbstractIntegrationTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void publicEndpoint_doesNotRequireAToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void protectedEndpoint_withoutToken_returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/items"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void protectedEndpoint_withGarbageToken_returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/items").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void protectedEndpoint_withValidToken_passesTheSecurityLayer() throws Exception {
        String token = jwtService.createAccessToken(new AuthUser(UUID.randomUUID(), "staff@logivault.test", Role.STAFF));

        mockMvc.perform(get("/api/v1/items").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
