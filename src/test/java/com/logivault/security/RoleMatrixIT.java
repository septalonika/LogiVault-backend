package com.logivault.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logivault.support.AbstractIntegrationTest;
import com.logivault.user.Role;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * One row per endpoint in the spec's endpoint table (section 3). Bodies are valid so that request
 * validation, which runs before method security, cannot hide an authorization result.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoleMatrixIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Password123!";

    enum Access { PUBLIC, ANY, ADMIN }

    record Endpoint(HttpMethod method, String path, Access access, String body) {
        @Override
        public String toString() {
            return method + " " + path + " [" + access + "]";
        }
    }

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String staffToken;

    @BeforeAll
    void createTokens() throws Exception {
        adminToken = testData.loginToken(Role.ADMIN, PASSWORD);
        staffToken = testData.loginToken(Role.STAFF, PASSWORD);
    }

    static Stream<Endpoint> endpoints() {
        String id = UUID.randomUUID().toString();
        String sku = "NOSUCH-" + UUID.randomUUID();
        String itemBody = "{\"name\":\"Matrix Item\",\"basePrice\":10}";
        String variantBody = "{\"sku\":\"MX-" + UUID.randomUUID() + "\",\"name\":\"V\"}";
        String orderBody = "{\"lines\":[{\"variantId\":\"" + id + "\",\"qty\":1}]}";
        String userBody = "{\"name\":\"Matrix\",\"email\":\"matrix-" + UUID.randomUUID()
                + "@logivault.test\",\"password\":\"StaffPass123\",\"role\":\"STAFF\"}";
        return List.of(
                new Endpoint(HttpMethod.POST, "/api/v1/auth/login", Access.PUBLIC, "{\"email\":\"nobody@logivault.test\",\"password\":\"x\"}"),
                new Endpoint(HttpMethod.POST, "/api/v1/auth/refresh", Access.PUBLIC, "{\"refreshToken\":\"nope\"}"),
                new Endpoint(HttpMethod.POST, "/api/v1/auth/logout", Access.ANY, "{\"refreshToken\":\"nope\"}"),

                new Endpoint(HttpMethod.GET, "/api/v1/users/me", Access.ANY, null),
                new Endpoint(HttpMethod.PUT, "/api/v1/users/me/password", Access.ANY, "{\"oldPassword\":\"wrong\",\"newPassword\":\"newpass123\"}"),
                new Endpoint(HttpMethod.GET, "/api/v1/users", Access.ADMIN, null),
                new Endpoint(HttpMethod.POST, "/api/v1/users", Access.ADMIN, userBody),
                new Endpoint(HttpMethod.GET, "/api/v1/users/" + id, Access.ADMIN, null),
                new Endpoint(HttpMethod.PUT, "/api/v1/users/" + id, Access.ADMIN, "{\"name\":\"X\",\"role\":\"STAFF\"}"),
                new Endpoint(HttpMethod.PATCH, "/api/v1/users/" + id + "/status", Access.ADMIN, "{\"active\":true}"),
                new Endpoint(HttpMethod.POST, "/api/v1/users/" + id + "/reset-password", Access.ADMIN, "{\"newPassword\":\"newpass123\"}"),

                new Endpoint(HttpMethod.GET, "/api/v1/items", Access.ANY, null),
                new Endpoint(HttpMethod.POST, "/api/v1/items", Access.ADMIN, itemBody),
                new Endpoint(HttpMethod.GET, "/api/v1/items/" + id, Access.ANY, null),
                new Endpoint(HttpMethod.PUT, "/api/v1/items/" + id, Access.ADMIN, itemBody),
                new Endpoint(HttpMethod.DELETE, "/api/v1/items/" + id, Access.ADMIN, null),
                new Endpoint(HttpMethod.PATCH, "/api/v1/items/" + id + "/activate", Access.ADMIN, null),
                new Endpoint(HttpMethod.GET, "/api/v1/items/" + id + "/variants", Access.ANY, null),
                new Endpoint(HttpMethod.POST, "/api/v1/items/" + id + "/variants", Access.ADMIN, variantBody),

                new Endpoint(HttpMethod.GET, "/api/v1/variants/" + id, Access.ANY, null),
                new Endpoint(HttpMethod.GET, "/api/v1/variants/sku/" + sku, Access.ANY, null),
                new Endpoint(HttpMethod.PUT, "/api/v1/variants/" + id, Access.ADMIN, variantBody),
                new Endpoint(HttpMethod.DELETE, "/api/v1/variants/" + id, Access.ADMIN, null),
                new Endpoint(HttpMethod.PATCH, "/api/v1/variants/" + id + "/activate", Access.ADMIN, null),
                new Endpoint(HttpMethod.POST, "/api/v1/variants/" + id + "/stock-in", Access.ANY, "{\"qty\":1,\"note\":\"x\"}"),
                new Endpoint(HttpMethod.POST, "/api/v1/variants/" + id + "/adjust", Access.ADMIN, "{\"delta\":1,\"reason\":\"x\"}"),
                new Endpoint(HttpMethod.GET, "/api/v1/variants/" + id + "/movements", Access.ANY, null),
                new Endpoint(HttpMethod.GET, "/api/v1/stock/low", Access.ANY, null),

                new Endpoint(HttpMethod.POST, "/api/v1/orders", Access.ANY, orderBody),
                new Endpoint(HttpMethod.GET, "/api/v1/orders", Access.ANY, null),
                new Endpoint(HttpMethod.GET, "/api/v1/orders/" + id, Access.ANY, null),
                new Endpoint(HttpMethod.POST, "/api/v1/orders/" + id + "/cancel", Access.ANY, "{\"reason\":\"x\"}"),

                new Endpoint(HttpMethod.GET, "/actuator/health", Access.PUBLIC, null),
                new Endpoint(HttpMethod.GET, "/v3/api-docs", Access.PUBLIC, null)
        ).stream();
    }

    static Stream<Endpoint> adminEndpoints() {
        return endpoints().filter(endpoint -> endpoint.access() == Access.ADMIN);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    void withoutToken(Endpoint endpoint) throws Exception {
        MockHttpServletResponse response = call(endpoint, null);

        if (endpoint.access() == Access.PUBLIC) {
            assertThat(rejectedBySecurity(response)).as("public endpoint must not be blocked").isFalse();
        } else {
            assertThat(response.getStatus()).isEqualTo(401);
            assertProblemWithCode(response, "UNAUTHORIZED");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    void asStaff(Endpoint endpoint) throws Exception {
        MockHttpServletResponse response = call(endpoint, staffToken);

        if (endpoint.access() == Access.ADMIN) {
            assertThat(response.getStatus()).isEqualTo(403);
            assertProblemWithCode(response, "FORBIDDEN");
        } else {
            assertThat(rejectedBySecurity(response)).as("STAFF is allowed here").isFalse();
        }
    }

    // Authorization must win over validation: an empty body from STAFF is still a 403, not a 400.
    @ParameterizedTest(name = "{0}")
    @MethodSource("adminEndpoints")
    void asStaffWithInvalidBody(Endpoint endpoint) throws Exception {
        Endpoint emptyBody = new Endpoint(endpoint.method(), endpoint.path(), endpoint.access(),
                endpoint.body() == null ? null : "{}");

        MockHttpServletResponse response = call(emptyBody, staffToken);

        assertThat(response.getStatus()).isEqualTo(403);
        assertProblemWithCode(response, "FORBIDDEN");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    void asAdmin(Endpoint endpoint) throws Exception {
        assertThat(rejectedBySecurity(call(endpoint, adminToken))).as("ADMIN is allowed everywhere").isFalse();
    }

    private MockHttpServletResponse call(Endpoint endpoint, String token) throws Exception {
        MockHttpServletRequestBuilder builder = request(endpoint.method(), endpoint.path());
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (endpoint.body() != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(endpoint.body());
        }
        return mockMvc.perform(builder).andReturn().getResponse();
    }

    // 401/403 produced by the security layer carry these two codes; business 401s (bad credentials) do not.
    private boolean rejectedBySecurity(MockHttpServletResponse response) throws Exception {
        if (response.getStatus() != 401 && response.getStatus() != 403) {
            return false;
        }
        String code = codeOf(response);
        return "UNAUTHORIZED".equals(code) || "FORBIDDEN".equals(code);
    }

    private void assertProblemWithCode(MockHttpServletResponse response, String expectedCode) throws Exception {
        assertThat(response.getContentType()).contains("application/problem+json");
        assertThat(codeOf(response)).isEqualTo(expectedCode);
    }

    private String codeOf(MockHttpServletResponse response) throws Exception {
        String body = response.getContentAsString();
        if (body.isBlank()) {
            return null;
        }
        JsonNode json = objectMapper.readTree(body);
        return json.hasNonNull("code") ? json.get("code").asText() : null;
    }
}
