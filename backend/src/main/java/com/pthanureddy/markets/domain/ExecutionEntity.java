package com.pthanureddy.markets.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "executions")
public class ExecutionEntity {
    @Id
    @Column(length = 80, nullable = false)
    private String executionId;

    @Column(length = 80, nullable = false)
    private String accountId;

    @Column(length = 40, nullable = false)
    private String instrument;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private AssetClass assetClass;

    @Enumerated(EnumType.STRING)
    @Column(length = 8, nullable = false)
    private Side side;

    @Column(precision = 24, scale = 6, nullable = false)
    private BigDecimal quantity;

    @Column(precision = 24, scale = 6, nullable = false)
    private BigDecimal price;

    @Column(length = 3, nullable = false)
    private String currency;

    @Column(nullable = false)
    private Instant executedAt;

    @Column(nullable = false)
    private Instant receivedAt;

    protected ExecutionEntity() {}

    public ExecutionEntity(
            String executionId,
            String accountId,
            String instrument,
            AssetClass assetClass,
            Side side,
            BigDecimal quantity,
            BigDecimal price,
            String currency,
            Instant executedAt,
            Instant receivedAt) {
        this.executionId = executionId;
        this.accountId = accountId;
        this.instrument = instrument;
        this.assetClass = assetClass;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.currency = currency;
        this.executedAt = executedAt;
        this.receivedAt = receivedAt;
    }

    public String getExecutionId() { return executionId; }
    public String getAccountId() { return accountId; }
    public String getInstrument() { return instrument; }
    public AssetClass getAssetClass() { return assetClass; }
    public Side getSide() { return side; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
    public Instant getExecutedAt() { return executedAt; }
    public Instant getReceivedAt() { return receivedAt; }
}

