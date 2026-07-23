package com.pthanureddy.markets.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PositionEntityTest {
    private static final BigDecimal LIMIT = new BigDecimal("1000000");

    @Test
    void buyIncreasesSignedQuantityAndNotional() {
        var position = new PositionEntity("ACC-1", "AAPL", AssetClass.EQUITY);

        position.apply(Side.BUY, new BigDecimal("100"), new BigDecimal("180"), LIMIT, Instant.parse("2026-07-23T10:00:00Z"));

        assertThat(position.getSignedQuantity()).isEqualByComparingTo("100");
        assertThat(position.getNetNotional()).isEqualByComparingTo("18000");
        assertThat(position.isLimitBreached()).isFalse();
    }

    @Test
    void sellCreatesShortPosition() {
        var position = new PositionEntity("ACC-1", "ESU6", AssetClass.FUTURE);

        position.apply(Side.SELL, new BigDecimal("3"), new BigDecimal("6200"), LIMIT, Instant.now());

        assertThat(position.getSignedQuantity()).isEqualByComparingTo("-3");
        assertThat(position.getNetNotional()).isEqualByComparingTo("-18600");
    }

    @Test
    void offsettingExecutionReducesPosition() {
        var position = new PositionEntity("ACC-1", "MSFT", AssetClass.EQUITY);
        position.apply(Side.BUY, new BigDecimal("100"), new BigDecimal("400"), LIMIT, Instant.now());

        position.apply(Side.SELL, new BigDecimal("40"), new BigDecimal("410"), LIMIT, Instant.now());

        assertThat(position.getSignedQuantity()).isEqualByComparingTo("60");
        assertThat(position.getNetNotional()).isEqualByComparingTo("24600");
    }

    @Test
    void exactLimitDoesNotBreach() {
        var position = new PositionEntity("ACC-1", "LIMIT", AssetClass.EQUITY);

        position.apply(Side.BUY, new BigDecimal("1000"), new BigDecimal("1000"), LIMIT, Instant.now());

        assertThat(position.isLimitBreached()).isFalse();
    }

    @Test
    void absoluteShortNotionalCanBreachLimit() {
        var position = new PositionEntity("ACC-1", "LIMIT", AssetClass.EQUITY);

        position.apply(Side.SELL, new BigDecimal("1001"), new BigDecimal("1000"), LIMIT, Instant.now());

        assertThat(position.isLimitBreached()).isTrue();
    }
}

