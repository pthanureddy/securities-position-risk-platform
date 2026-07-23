package com.pthanureddy.markets.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboxEventEntityTest {
    @Test
    void marksPublishedWithTimestamp() {
        var event = event();
        Instant publishedAt = Instant.parse("2026-07-23T11:00:00Z");

        event.markPublished(publishedAt);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isEqualTo(publishedAt);
    }

    @Test
    void remainsPendingUntilMaximumFailures() {
        var event = event();

        event.markFailed(2);

        assertThat(event.getAttempts()).isOne();
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    void movesToFailedAtMaximumAttempts() {
        var event = event();

        event.markFailed(2);
        event.markFailed(2);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
    }

    private OutboxEventEntity event() {
        return new OutboxEventEntity(UUID.randomUUID(), "EXEC-1", "ExecutionAccepted.v1", "{}", Instant.now());
    }
}

