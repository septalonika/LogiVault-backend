package com.logivault.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logivault.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIT extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@logivault.test";
    private static final String ADMIN_PASSWORD = "Admin12345!";

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void login_withSeededAdminCredentials_returnsBothTokens() throws Exception {
        mockMvc.perform(loginRequest(ADMIN_EMAIL, ADMIN_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"));
    }

    @Test
    void login_withWrongPasswordOrUnknownEmail_returnsTheSame401Body() throws Exception {
        String wrongPasswordBody = mockMvc.perform(loginRequest(ADMIN_EMAIL, "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn().getResponse().getContentAsString();

        String unknownEmailBody = mockMvc.perform(loginRequest("nobody@logivault.test", "whatever123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn().getResponse().getContentAsString();

        JsonNode wrongPassword = objectMapper.readTree(wrongPasswordBody);
        JsonNode unknownEmail = objectMapper.readTree(unknownEmailBody);
        assertThat(wrongPassword.get("code")).isEqualTo(unknownEmail.get("code"));
        assertThat(wrongPassword.get("message")).isEqualTo(unknownEmail.get("message"));
    }

    @Test
    void refresh_rotatesTheToken_andRejectsTheOldOneAfterwards() throws Exception {
        String firstRefreshToken = login(ADMIN_EMAIL, ADMIN_PASSWORD).get("refreshToken").asText();

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshBody(firstRefreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        String rotatedToken = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                .get("data").get("refreshToken").asText();
        assertThat(rotatedToken).isNotEqualTo(firstRefreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshBody(firstRefreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void logout_revokesTheRefreshToken() throws Exception {
        String refreshToken = login(ADMIN_EMAIL, ADMIN_PASSWORD).get("refreshToken").asText();
        String accessToken = loginAccessToken();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshBody(refreshToken))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshBody(refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void accessToken_fromLogin_worksOnAProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/items").header("Authorization", "Bearer " + loginAccessToken()))
                .andExpect(status().isOk());
    }

    private String loginAccessToken() throws Exception {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD).get("accessToken").asText();
    }

    private JsonNode login(String email, String password) throws Exception {
        String body = mockMvc.perform(loginRequest(email, password))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("data");
    }

    private org.springframework.test.web.servlet.RequestBuilder loginRequest(String email, String password) throws Exception {
        return post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginBody(email, password)));
    }

    private record LoginBody(String email, String password) {
    }

    private record RefreshBody(String refreshToken) {
    }
}
