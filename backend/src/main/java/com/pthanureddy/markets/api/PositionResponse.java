package com.pthanureddy.markets.api;

import com.pthanureddy.markets.domain.AssetClass;
import java.math.BigDecimal;
import java.time.Instant;

public record PositionResponse(
        String accountId,
        String instrument,
        AssetClass assetClass,
        BigDecimal signedQuantity,
        BigDecimal lastPrice,
        BigDecimal netNotional,
        boolean limitBreached,
        Instant updatedAt) {}

