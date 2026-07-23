package com.pthanureddy.markets.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {
    @Id
    private UUID id;

    @Column(length = 80, nullable = false)
    private String aggregateId;

    @Column(length = 60, nullable = false)
    private String eventType;

    @Column(columnDefinition = "text", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant publishedAt;

    protected OutboxEventEntity() {}

    public OutboxEventEntity(UUID id, String aggregateId, String eventType, String payload, Instant createdAt) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.attempts = 0;
        this.createdAt = createdAt;
    }

    public void markPublished(Instant now) {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = now;
    }

    public void markFailed(int maximumAttempts) {
        this.attempts++;
        if (this.attempts >= maximumAttempts) {
            this.status = OutboxStatus.FAILED;
        }
    }

    public UUID getId() { return id; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public OutboxStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
}

