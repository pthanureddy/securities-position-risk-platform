package com.pthanureddy.markets.api;

import com.pthanureddy.markets.domain.AssetClass;
import com.pthanureddy.markets.domain.Side;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record ExecutionRequest(
        @NotBlank @Size(max = 80) String executionId,
        @NotBlank @Size(max = 80) String accountId,
        @NotBlank @Pattern(regexp = "[A-Z0-9._-]{1,40}") String instrument,
        @NotNull AssetClass assetClass,
        @NotNull Side side,
        @NotNull @DecimalMin(value = "0.000001") BigDecimal quantity,
        @NotNull @DecimalMin(value = "0.000001") BigDecimal price,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
        @NotNull Instant executedAt) {}

