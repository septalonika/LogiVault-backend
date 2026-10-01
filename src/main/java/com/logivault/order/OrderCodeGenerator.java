package com.logivault.order;

import com.logivault.config.LogiVaultProperties;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class OrderCodeGenerator {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final EntityManager entityManager;
    private final Clock clock;
    private final ZoneId businessZone;

    public OrderCodeGenerator(EntityManager entityManager, Clock clock, LogiVaultProperties properties) {
        this.entityManager = entityManager;
        this.clock = clock;
        this.businessZone = properties.businessZone();
    }

    // Upserts today's counter and returns the new value in one round trip, so concurrent calls on
    // the same day each get a unique sequence number without a separate read-then-write race.
    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        LocalDate today = LocalDate.now(clock.withZone(businessZone));

        Number seq = (Number) entityManager.createNativeQuery("""
                insert into order_code_counters (day, last_seq) values (:day, 1)
                on conflict (day) do update set last_seq = order_code_counters.last_seq + 1
                returning last_seq
                """)
                .setParameter("day", today)
                .getSingleResult();

        return "ORD-" + today.format(DAY_FORMAT) + "-" + String.format("%04d", seq.intValue());
    }
}
