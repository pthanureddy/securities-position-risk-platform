package com.pthanureddy.markets.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pthanureddy.markets.api.RiskSummaryResponse;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.cache.redis-enabled", havingValue = "true")
public class RedisRiskSnapshotCache implements RiskSnapshotCache {
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisRiskSnapshotCache(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public void put(RiskSummaryResponse summary) {
        try {
            redis.opsForValue()
                    .set("risk-summary:" + summary.accountId(), objectMapper.writeValueAsString(summary), Duration.ofMinutes(5));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize risk snapshot", exception);
        }
    }
}

