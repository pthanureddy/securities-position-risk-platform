package com.pthanureddy.markets.cache;

import com.pthanureddy.markets.api.RiskSummaryResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.cache.redis-enabled",
        havingValue = "false",
        matchIfMissing = true)
public class NoOpRiskSnapshotCache implements RiskSnapshotCache {
    @Override
    public void put(RiskSummaryResponse summary) {
        // The default local profile is self-contained; Redis is an opt-in runtime adapter.
    }
}
