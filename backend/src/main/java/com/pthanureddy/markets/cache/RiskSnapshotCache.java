package com.pthanureddy.markets.cache;

import com.pthanureddy.markets.api.RiskSummaryResponse;

public interface RiskSnapshotCache {
    void put(RiskSummaryResponse summary);
}

