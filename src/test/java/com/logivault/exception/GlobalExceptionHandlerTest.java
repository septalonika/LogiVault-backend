package com.logivault.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new DummyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void businessException_rendersErrorEnvelopeWithCodeAndDetails() throws Exception {
        mvc.perform(post("/test/orders"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.message").value("Not enough stock for 1 line(s)"))
                .andExpect(jsonPath("$.details.lines", hasSize(1)))
                .andExpect(jsonPath("$.details.lines[0].sku").value("TS-RED-M"))
                .andExpect(jsonPath("$.details.lines[0].requested").value(5))
                .andExpect(jsonPath("$.details.lines[0].available").value(3));
    }

    @Test
    void invalidRequestBody_returns400WithFieldErrors() throws Exception {
        mvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"qty\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')].message").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'qty')].message").exists());
    }

    @Test
    void malformedJson_returns400ValidationError() throws Exception {
        mvc.perform(post("/test/validate").contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void invalidUuidInPath_returns400() throws Exception {
        mvc.perform(get("/test/items/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void wrongHttpMethod_returns405WithStatusNameAsCode() throws Exception {
        mvc.perform(get("/test/orders"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void accessDenied_returns403Forbidden() throws Exception {
        mvc.perform(get("/test/admin-only"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void optimisticLock_returns409ConcurrentModification() throws Exception {
        mvc.perform(get("/test/optimistic"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"));
    }

    @Test
    void unexpectedException_returns500WithoutLeakingDetails() throws Exception {
        mvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Unexpected server error"))
                .andExpect(content().string(not(containsString("secret-internal-detail"))));
    }

    record DummyRequest(@NotBlank String name, @Positive int qty) {
    }

    @RestController
    static class DummyController {

        @PostMapping("/test/orders")
        void insufficientStock() {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, "Not enough stock for 1 line(s)",
                    Map.of("lines", List.of(Map.of(
                            "variantId", UUID.fromString("00000000-0000-0000-0000-000000000001"),
                            "sku", "TS-RED-M", "requested", 5, "available", 3))));
        }

        @PostMapping("/test/validate")
        void validate(@Valid @RequestBody DummyRequest request) {
        }

        @GetMapping("/test/items/{id}")
        void item(@PathVariable UUID id) {
        }

        @GetMapping("/test/admin-only")
        void adminOnly() {
            throw new AccessDeniedException("Access Denied");
        }

        @GetMapping("/test/optimistic")
        void optimistic() {
            throw new OptimisticLockingFailureException("Row was updated by another transaction");
        }

        @GetMapping("/test/boom")
        void boom() {
            throw new IllegalStateException("secret-internal-detail");
        }
    }
}
