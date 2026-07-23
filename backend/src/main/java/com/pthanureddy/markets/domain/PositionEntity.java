package com.pthanureddy.markets.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "positions",
        uniqueConstraints = @UniqueConstraint(name = "uk_position_account_instrument", columnNames = {"account_id", "instrument"}))
public class PositionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private long version;

    @Column(name = "account_id", length = 80, nullable = false)
    private String accountId;

    @Column(length = 40, nullable = false)
    private String instrument;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private AssetClass assetClass;

    @Column(precision = 24, scale = 6, nullable = false)
    private BigDecimal signedQuantity;

    @Column(precision = 24, scale = 6, nullable = false)
    private BigDecimal lastPrice;

    @Column(precision = 24, scale = 6, nullable = false)
    private BigDecimal netNotional;

    @Column(nullable = false)
    private boolean limitBreached;

    @Column(nullable = false)
    private Instant updatedAt;

    protected PositionEntity() {}

    public PositionEntity(String accountId, String instrument, AssetClass assetClass) {
        this.accountId = accountId;
        this.instrument = instrument;
        this.assetClass = assetClass;
        this.signedQuantity = BigDecimal.ZERO;
        this.lastPrice = BigDecimal.ZERO;
        this.netNotional = BigDecimal.ZERO;
        this.limitBreached = false;
        this.updatedAt = Instant.EPOCH;
    }

    public void apply(Side side, BigDecimal quantity, BigDecimal price, BigDecimal absoluteNotionalLimit, Instant now) {
        BigDecimal signedDelta = quantity.multiply(BigDecimal.valueOf(side.multiplier()));
        this.signedQuantity = this.signedQuantity.add(signedDelta);
        this.lastPrice = price;
        this.netNotional = this.signedQuantity.multiply(price);
        this.limitBreached = this.netNotional.abs().compareTo(absoluteNotionalLimit) > 0;
        this.updatedAt = now;
    }

    public Long getId() { return id; }
    public long getVersion() { return version; }
    public String getAccountId() { return accountId; }
    public String getInstrument() { return instrument; }
    public AssetClass getAssetClass() { return assetClass; }
    public BigDecimal getSignedQuantity() { return signedQuantity; }
    public BigDecimal getLastPrice() { return lastPrice; }
    public BigDecimal getNetNotional() { return netNotional; }
    public boolean isLimitBreached() { return limitBreached; }
    public Instant getUpdatedAt() { return updatedAt; }
}

