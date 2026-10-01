package com.logivault.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logivault.dto.WebResponse;
import com.logivault.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;

// Shared by RestAuthenticationEntryPoint and RestAccessDeniedHandler: both run inside the
// security filter chain, before MVC, so they write the error envelope themselves.
final class SecurityErrorWriter {

    private SecurityErrorWriter() {
    }

    static void write(HttpServletResponse response, ObjectMapper objectMapper, ErrorCode code) throws IOException {
        int status = code.getStatus().value();
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                WebResponse.error(status, code.getDefaultMessage(), code.name(), null, null));
    }
}
