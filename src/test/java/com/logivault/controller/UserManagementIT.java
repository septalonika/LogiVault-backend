package com.logivault.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logivault.entity.Role;
import com.logivault.entity.User;
import com.logivault.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserManagementIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminCreatesStaff_whoCanThenLogIn_andResponseHasNoPasswordHash() throws Exception {
        String admin = adminToken();
        String email = testData.uniqueEmail();

        mockMvc.perform(json(post("/api/v1/users"), admin,
                        Map.of("name", "New Staff", "email", email, "password", "StaffPass123", "role", "STAFF")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.role").value("STAFF"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(content().string(not(containsString("password"))));

        mockMvc.perform(login(email, "StaffPass123")).andExpect(status().isOk());
    }

    @Test
    void duplicateEmail_inDifferentCase_returns409() throws Exception {
        String admin = adminToken();
        String email = testData.uniqueEmail();
        create(admin, email, "STAFF");

        mockMvc.perform(json(post("/api/v1/users"), admin,
                        Map.of("name", "Dup", "email", email.toUpperCase(), "password", "StaffPass123", "role", "STAFF")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void deactivatedStaff_cannotLogIn_andRefreshTokenIsRejected() throws Exception {
        String admin = adminToken();
        String email = testData.uniqueEmail();
        UUID staffId = create(admin, email, "STAFF");
        String refreshToken = objectMapper.readTree(mockMvc.perform(login(email, "StaffPass123"))
                .andReturn().getResponse().getContentAsString()).get("refreshToken").asText();

        mockMvc.perform(json(patch("/api/v1/users/" + staffId + "/status"), admin, Map.of("active", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(login(email, "StaffPass123")).andExpect(status().isUnauthorized());
        mockMvc.perform(json(post("/api/v1/auth/refresh"), null, Map.of("refreshToken", refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCannotDeactivateOrDemoteSelf() throws Exception {
        User adminUser = testData.createUser(Role.ADMIN, PASSWORD);
        String admin = testData.accessTokenFor(adminUser, PASSWORD);

        mockMvc.perform(json(patch("/api/v1/users/" + adminUser.getId() + "/status"), admin, Map.of("active", false)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SELF_MODIFICATION_NOT_ALLOWED"));

        mockMvc.perform(json(put("/api/v1/users/" + adminUser.getId()), admin, Map.of("name", "Me", "role", "STAFF")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SELF_MODIFICATION_NOT_ALLOWED"));

        mockMvc.perform(json(put("/api/v1/users/" + adminUser.getId()), admin, Map.of("name", "Renamed", "role", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void resetPassword_changesLogin_andRevokesRefreshTokens() throws Exception {
        String admin = adminToken();
        String email = testData.uniqueEmail();
        UUID staffId = create(admin, email, "STAFF");
        String refreshToken = objectMapper.readTree(mockMvc.perform(login(email, "StaffPass123"))
                .andReturn().getResponse().getContentAsString()).get("refreshToken").asText();

        mockMvc.perform(json(post("/api/v1/users/" + staffId + "/reset-password"), admin, Map.of("newPassword", "BrandNew123")))
                .andExpect(status().isNoContent());

        mockMvc.perform(login(email, "StaffPass123")).andExpect(status().isUnauthorized());
        mockMvc.perform(login(email, "BrandNew123")).andExpect(status().isOk());
        mockMvc.perform(json(post("/api/v1/auth/refresh"), null, Map.of("refreshToken", refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_filtersByQueryRoleAndActive() throws Exception {
        String admin = adminToken();
        String marker = "zq" + System.nanoTime();
        UUID staffId = create(admin, marker + "@logivault.test", "STAFF");
        create(admin, marker + "-b@logivault.test", "ADMIN");
        mockMvc.perform(json(patch("/api/v1/users/" + staffId + "/status"), admin, Map.of("active", false)));

        mockMvc.perform(get("/api/v1/users?q=" + marker).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/v1/users?q=" + marker + "&role=ADMIN").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/users?q=" + marker + "&active=false").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(staffId.toString()));
    }

    @Test
    void getById_unknownUser_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + UUID.randomUUID()).header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void invalidCreateBody_returns400() throws Exception {
        mockMvc.perform(json(post("/api/v1/users"), adminToken(),
                        Map.of("name", "X", "email", "not-an-email", "password", "short", "role", "STAFF")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void staff_callingAdminEndpoints_gets403() throws Exception {
        String staff = testData.loginToken(Role.STAFF, PASSWORD);
        String id = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + staff)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/users/" + id).header("Authorization", "Bearer " + staff)).andExpect(status().isForbidden());
        mockMvc.perform(json(post("/api/v1/users"), staff,
                        Map.of("name", "X", "email", testData.uniqueEmail(), "password", "StaffPass123", "role", "STAFF")))
                .andExpect(status().isForbidden());
        mockMvc.perform(json(put("/api/v1/users/" + id), staff, Map.of("name", "X", "role", "STAFF")))
                .andExpect(status().isForbidden());
        mockMvc.perform(json(patch("/api/v1/users/" + id + "/status"), staff, Map.of("active", false)))
                .andExpect(status().isForbidden());
        mockMvc.perform(json(post("/api/v1/users/" + id + "/reset-password"), staff, Map.of("newPassword", "BrandNew123")))
                .andExpect(status().isForbidden());
    }

    private String adminToken() throws Exception {
        return testData.loginToken(Role.ADMIN, PASSWORD);
    }

    private UUID create(String admin, String email, String role) throws Exception {
        String body = mockMvc.perform(json(post("/api/v1/users"), admin,
                        Map.of("name", "Test User", "email", email, "password", "StaffPass123", "role", role)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(body);
        return UUID.fromString(created.get("id").asText());
    }

    private MockHttpServletRequestBuilder login(String email, String password) throws Exception {
        return json(post("/api/v1/auth/login"), null, Map.of("email", email, "password", password));
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String token, Object body) throws Exception {
        MockHttpServletRequestBuilder request = builder.contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
        return token == null ? request : request.header("Authorization", "Bearer " + token);
    }
}
