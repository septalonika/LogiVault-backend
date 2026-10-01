package com.logivault.support;

import org.springframework.stereotype.Component;

import java.util.UUID;

// Gains real builder methods (user, item, variant, login-as...) as those entities land in T-05+.
@Component
public class TestDataFactory {

    public String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@logivault.test";
    }
}
