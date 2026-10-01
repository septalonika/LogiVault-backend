package com.logivault.user;

import com.logivault.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void seedMigration_createsExactlyOneAdmin() {
        // Other ITs share this database and create their own admins, so only the seeded one is checked.
        assertThat(userRepository.findByEmailIgnoreCase("admin@logivault.test"))
                .hasValueSatisfying(admin -> assertThat(admin.getRole()).isEqualTo(Role.ADMIN));
    }

    @Test
    void findByEmailIgnoreCase_matchesRegardlessOfCase() {
        String email = testData.uniqueEmail();
        userRepository.save(newUser(email));

        assertThat(userRepository.findByEmailIgnoreCase(email.toUpperCase())).isPresent();
        assertThat(userRepository.existsByEmailIgnoreCase(email.toUpperCase())).isTrue();
    }

    @Test
    void save_rejectsDuplicateEmailRegardlessOfCase() {
        String email = testData.uniqueEmail();
        userRepository.saveAndFlush(newUser(email));

        assertThatThrownBy(() -> userRepository.saveAndFlush(newUser(email.toUpperCase())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static User newUser(String email) {
        return User.builder()
                .name("Test Staff")
                .email(email)
                .passwordHash("not-a-real-hash")
                .role(Role.STAFF)
                .active(true)
                .build();
    }
}
