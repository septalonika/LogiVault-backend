package com.logivault.db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

// Email/password come in via spring.flyway.placeholders (set from logivault.admin.* in application.yml),
// so this stays in sync with how the rest of the app reads .env, instead of touching System.getenv() directly.
public class V2__SeedAdmin extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        var placeholders = context.getConfiguration().getPlaceholders();
        String email = placeholders.get("adminEmail");
        String password = placeholders.get("adminPassword");

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "LOGIVAULT_ADMIN_EMAIL and LOGIVAULT_ADMIN_PASSWORD must be set to seed the first admin");
        }

        var connection = context.getConnection();

        try (PreparedStatement check = connection.prepareStatement(
                "select count(*) from users where role = 'ADMIN'")) {
            try (ResultSet rs = check.executeQuery()) {
                rs.next();
                if (rs.getLong(1) > 0) {
                    return;
                }
            }
        }

        String hash = new BCryptPasswordEncoder(12).encode(password);

        try (PreparedStatement insert = connection.prepareStatement("""
                insert into users (id, name, email, password_hash, role, active, created_at, updated_at)
                values (?, 'Administrator', ?, ?, 'ADMIN', true, now(), now())
                """)) {
            insert.setObject(1, UUID.randomUUID());
            insert.setString(2, email.toLowerCase());
            insert.setString(3, hash);
            insert.executeUpdate();
        }
    }
}
