package com.pthanureddy.markets.api;

import java.math.BigDecimal;

public record ExecutionResponse(
        String executionId,
        boolean accepted,
        boolean duplicate,
        String accountId,
        String instrument,
        BigDecimal signedQuantity,
        BigDecimal netNotional,
        boolean limitBreached) {}

