package com.logivault.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logivault.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.LinkedHashMap;

// Shared by RestAuthenticationEntryPoint and RestAccessDeniedHandler: both run inside the
// security filter chain, before MVC, so they can't return a ProblemDetail via a controller.
final class SecurityProblemWriter {

    private SecurityProblemWriter() {
    }

    static void write(HttpServletResponse response, HttpServletRequest request, ObjectMapper objectMapper,
                       ErrorCode code) throws IOException {
        var body = new LinkedHashMap<String, Object>();
        body.put("type", "about:blank");
        body.put("title", code.getStatus().getReasonPhrase());
        body.put("status", code.getStatus().value());
        body.put("detail", code.getDefaultMessage());
        body.put("instance", request.getRequestURI());
        body.put("code", code.name());

        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
