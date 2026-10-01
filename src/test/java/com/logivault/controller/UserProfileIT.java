package com.logivault.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logivault.entity.Role;
import com.logivault.entity.User;
import com.logivault.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.RequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserProfileIT extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void me_returnsTheLoggedInUser() throws Exception {
        String rawPassword = "StaffPass123!";
        User user = testData.createUser(Role.STAFF, rawPassword);
        String accessToken = testData.accessTokenFor(user, rawPassword);

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(user.getEmail()))
                .andExpect(jsonPath("$.data.role").value("STAFF"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void changePassword_withWrongOldPassword_returns400() throws Exception {
        String rawPassword = "StaffPass123!";
        User user = testData.createUser(Role.STAFF, rawPassword);
        String accessToken = testData.accessTokenFor(user, rawPassword);

        mockMvc.perform(changePasswordRequest(accessToken, "wrong-password", "NewPassword123!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OLD_PASSWORD"));
    }

    @Test
    void changePassword_withSameNewPassword_returns400() throws Exception {
        String rawPassword = "StaffPass123!";
        User user = testData.createUser(Role.STAFF, rawPassword);
        String accessToken = testData.accessTokenFor(user, rawPassword);

        mockMvc.perform(changePasswordRequest(accessToken, rawPassword, rawPassword))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void changePassword_success_updatesCredentialsAndRevokesPreviousSessions() throws Exception {
        String oldPassword = "StaffPass123!";
        String newPassword = "NewStaffPass456!";
        User user = testData.createUser(Role.STAFF, oldPassword);

        JsonNode firstLogin = testData.loginAs(user.getEmail(), oldPassword);
        String accessToken = firstLogin.get("accessToken").asText();
        String firstRefreshToken = firstLogin.get("refreshToken").asText();

        mockMvc.perform(changePasswordRequest(accessToken, oldPassword, newPassword))
                .andExpect(status().isOk());

        mockMvc.perform(loginRequest(user.getEmail(), oldPassword))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        mockMvc.perform(loginRequest(user.getEmail(), newPassword))
                .andExpect(status().isOk());

        mockMvc.perform(refreshRequest(firstRefreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    private RequestBuilder changePasswordRequest(String accessToken, String oldPassword, String newPassword) throws Exception {
        return put("/api/v1/users/me/password")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ChangePasswordBody(oldPassword, newPassword)));
    }

    private RequestBuilder loginRequest(String email, String password) throws Exception {
        return post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginBody(email, password)));
    }

    private RequestBuilder refreshRequest(String refreshToken) throws Exception {
        return post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshBody(refreshToken)));
    }

    private record LoginBody(String email, String password) {
    }

    private record RefreshBody(String refreshToken) {
    }

    private record ChangePasswordBody(String oldPassword, String newPassword) {
    }
}
