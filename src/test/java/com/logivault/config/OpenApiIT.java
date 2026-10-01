package com.logivault.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logivault.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@TestPropertySource(properties = "springdoc.api-docs.enabled=true")
class OpenApiIT extends AbstractIntegrationTest {

    private static final List<String> HTTP_METHODS = List.of("get", "post", "put", "patch", "delete");

    @Autowired
    private ObjectMapper objectMapper;

    private JsonNode docs() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private List<JsonNode> operations(JsonNode docs) {
        List<JsonNode> ops = new ArrayList<>();
        for (JsonNode path : docs.get("paths")) {
            for (String method : HTTP_METHODS) {
                if (path.has(method)) {
                    ops.add(path.get(method));
                }
            }
        }
        return ops;
    }

    @Test
    void everyEndpointHasTagAndSummary() throws Exception {
        List<JsonNode> ops = operations(docs());

        assertThat(ops).hasSize(32);
        assertThat(ops).allSatisfy(op -> {
            assertThat(op.get("tags")).isNotEmpty();
            assertThat(op.get("summary").asText()).isNotBlank();
        });
    }

    @Test
    void bearerSchemeIsGlobalAndOnlyLoginAndRefreshArePublic() throws Exception {
        JsonNode docs = docs();

        assertThat(docs.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
        assertThat(docs.at("/security/0/bearerAuth")).isNotNull();
        for (Iterator<Map.Entry<String, JsonNode>> it = docs.get("paths").fields(); it.hasNext(); ) {
            var path = it.next();
            boolean isPublic = path.getKey().equals("/api/v1/auth/login") || path.getKey().equals("/api/v1/auth/refresh");
            JsonNode security = path.getValue().elements().next().get("security");
            if (isPublic) {
                assertThat(security).as(path.getKey()).isNotNull();
                assertThat(security).as(path.getKey()).isEmpty();
            } else if (!path.getKey().equals("/api/v1/auth/logout")) {
                assertThat(security).as(path.getKey()).isNull();
            }
        }
    }

    @Test
    void createOrderDocumentsInsufficientStockConflict() throws Exception {
        JsonNode responses = docs().at("/paths/~1api~1v1~1orders/post/responses");

        assertThat(responses.get("409").get("description").asText()).contains("INSUFFICIENT_STOCK");
        assertThat(responses.has("401")).isTrue();
        assertThat(responses.has("400")).isTrue();
        assertThat(responses.at("/409/content/application~1json/schema/$ref").asText())
                .isEqualTo("#/components/schemas/ErrorResponse");
        assertThat(docs().at("/components/schemas/ErrorResponse/properties/code")).isNotEmpty();
    }

    @Test
    void successResponsesAreWrappedInTheEnvelope() throws Exception {
        JsonNode docs = docs();
        String ref = docs.at("/paths/~1api~1v1~1orders/post/responses/201/content/*~1*/schema/$ref").asText();
        JsonNode envelope = docs.at("/components/schemas/" + ref.substring(ref.lastIndexOf('/') + 1) + "/properties");

        assertThat(envelope.has("status")).isTrue();
        assertThat(envelope.has("message")).isTrue();
        assertThat(envelope.at("/data/$ref").asText()).endsWith("/OrderResponse");
    }
}
