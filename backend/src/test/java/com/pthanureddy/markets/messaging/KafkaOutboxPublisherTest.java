package com.pthanureddy.markets.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.pthanureddy.markets.domain.OutboxEventEntity;
import com.pthanureddy.markets.domain.OutboxStatus;
import com.pthanureddy.markets.repository.OutboxEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class KafkaOutboxPublisherTest {
    @Mock OutboxEventRepository outbox;
    @Mock KafkaTemplate<String, String> kafka;

    @Test
    void marksEventPublishedAfterBrokerAcknowledgement() {
        OutboxEventEntity event = event();
        when(outbox.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)).thenReturn(List.of(event));
        when(kafka.send("execution-events-v1", "EXEC-1", "{}"))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, null)));
        var publisher = publisher(5);

        publisher.publishBatch();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isEqualTo(Instant.parse("2026-07-23T12:00:00Z"));
    }

    @Test
    void recordsFailureWithoutLosingPendingEvent() {
        OutboxEventEntity event = event();
        when(outbox.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)).thenReturn(List.of(event));
        when(kafka.send("execution-events-v1", "EXEC-1", "{}"))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));
        var publisher = publisher(2);

        publisher.publishBatch();

        assertThat(event.getAttempts()).isOne();
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    void deadLettersAfterConfiguredFailures() {
        OutboxEventEntity event = event();
        event.markFailed(2);
        when(outbox.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)).thenReturn(List.of(event));
        when(kafka.send("execution-events-v1", "EXEC-1", "{}"))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        publisher(2).publishBatch();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
    }

    private KafkaOutboxPublisher publisher(int maximumAttempts) {
        return new KafkaOutboxPublisher(
                outbox,
                kafka,
                Clock.fixed(Instant.parse("2026-07-23T12:00:00Z"), ZoneOffset.UTC),
                "execution-events-v1",
                maximumAttempts);
    }

    private OutboxEventEntity event() {
        return new OutboxEventEntity(
                UUID.randomUUID(), "EXEC-1", "ExecutionAccepted.v1", "{}", Instant.now());
    }
}

