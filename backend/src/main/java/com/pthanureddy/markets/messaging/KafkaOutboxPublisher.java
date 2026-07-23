package com.pthanureddy.markets.messaging;

import com.pthanureddy.markets.domain.OutboxStatus;
import com.pthanureddy.markets.repository.OutboxEventRepository;
import java.time.Clock;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.messaging.kafka-enabled", havingValue = "true")
public class KafkaOutboxPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaOutboxPublisher.class);

    private final OutboxEventRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final Clock clock;
    private final String topic;
    private final int maximumAttempts;

    @Autowired
    public KafkaOutboxPublisher(
            OutboxEventRepository outbox,
            KafkaTemplate<String, String> kafka,
            @Value("${app.messaging.topic:execution-events-v1}") String topic,
            @Value("${app.messaging.maximum-attempts:5}") int maximumAttempts) {
        this(outbox, kafka, Clock.systemUTC(), topic, maximumAttempts);
    }

    KafkaOutboxPublisher(
            OutboxEventRepository outbox,
            KafkaTemplate<String, String> kafka,
            Clock clock,
            String topic,
            int maximumAttempts) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.clock = clock;
        this.topic = topic;
        this.maximumAttempts = maximumAttempts;
    }

    @Scheduled(fixedDelayString = "${app.messaging.poll-delay-ms:1000}")
    @Transactional
    public void publishBatch() {
        outbox.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING).forEach(event -> {
            try {
                kafka.send(topic, event.getAggregateId(), event.getPayload()).get(5, TimeUnit.SECONDS);
                event.markPublished(clock.instant());
            } catch (Exception failure) {
                event.markFailed(maximumAttempts);
                LOG.warn("Could not publish outbox event {}", event.getId(), failure);
            }
        });
    }
}
