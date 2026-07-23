package com.pthanureddy.markets.api;

public record OutboxStatusResponse(long pending, long published, long failed) {}

