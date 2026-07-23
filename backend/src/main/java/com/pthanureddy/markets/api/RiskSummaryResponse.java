package com.pthanureddy.markets.api;

import java.math.BigDecimal;

public record RiskSummaryResponse(
        String accountId,
        int positionCount,
        long breachedPositions,
        BigDecimal grossNotional,
        BigDecimal netNotional) {}

