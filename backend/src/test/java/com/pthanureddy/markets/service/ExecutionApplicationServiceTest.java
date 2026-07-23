package com.pthanureddy.markets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pthanureddy.markets.api.ExecutionRequest;
import com.pthanureddy.markets.cache.RiskSnapshotCache;
import com.pthanureddy.markets.domain.AssetClass;
import com.pthanureddy.markets.domain.ExecutionEntity;
import com.pthanureddy.markets.domain.PositionEntity;
import com.pthanureddy.markets.domain.Side;
import com.pthanureddy.markets.repository.ExecutionRepository;
import com.pthanureddy.markets.repository.OutboxEventRepository;
import com.pthanureddy.markets.repository.PositionRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExecutionApplicationServiceTest {
    @Mock ExecutionRepository executions;
    @Mock PositionRepository positions;
    @Mock OutboxEventRepository outbox;
    @Mock RiskSnapshotCache cache;

    private ExecutionApplicationService service;
    private final Instant now = Instant.parse("2026-07-23T12:00:00Z");

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        service = new ExecutionApplicationService(
                executions,
                positions,
                outbox,
                cache,
                mapper,
                new SimpleMeterRegistry(),
                Clock.fixed(now, ZoneOffset.UTC),
                new BigDecimal("1000000"));
    }

    @Test
    void recordsExecutionPositionAndOutboxAtomically() {
        ExecutionRequest request = request("EXEC-1");
        when(executions.findById("EXEC-1")).thenReturn(Optional.empty());
        when(positions.findForUpdate("ACC-1", "AAPL")).thenReturn(Optional.empty());
        when(positions.findByAccountIdOrderByInstrumentAsc("ACC-1"))
                .thenAnswer(invocation -> List.of(position("100", "180")));

        var response = service.record(request);

        assertThat(response.accepted()).isTrue();
        assertThat(response.duplicate()).isFalse();
        assertThat(response.signedQuantity()).isEqualByComparingTo("100");
        verify(executions).save(any());
        verify(positions).save(any());
        verify(outbox).save(any());
        verify(cache).put(any());
    }

    @Test
    void duplicateExecutionIsNotAppliedAgain() {
        ExecutionRequest request = request("EXEC-1");
        var execution = new ExecutionEntity(
                "EXEC-1", "ACC-1", "AAPL", AssetClass.EQUITY, Side.BUY,
                new BigDecimal("100"), new BigDecimal("180"), "USD", now, now);
        PositionEntity position = position("100", "180");
        when(executions.findById("EXEC-1")).thenReturn(Optional.of(execution));
        when(positions.findForUpdate("ACC-1", "AAPL")).thenReturn(Optional.of(position));

        var response = service.record(request);

        assertThat(response.duplicate()).isTrue();
        assertThat(response.accepted()).isFalse();
        verify(executions, never()).save(any());
        verify(outbox, never()).save(any());
    }

    @Test
    void cacheFailureDoesNotRejectExecution() {
        when(executions.findById("EXEC-2")).thenReturn(Optional.empty());
        when(positions.findForUpdate("ACC-1", "AAPL")).thenReturn(Optional.empty());
        when(positions.findByAccountIdOrderByInstrumentAsc("ACC-1"))
                .thenAnswer(invocation -> List.of(position("100", "180")));
        doThrow(new IllegalStateException("redis down")).when(cache).put(any());

        var response = service.record(request("EXEC-2"));

        assertThat(response.accepted()).isTrue();
        verify(outbox).save(any());
    }

    @Test
    void summarizesGrossNetAndBreaches() {
        PositionEntity longPosition = position("100", "180");
        PositionEntity shortPosition = new PositionEntity("ACC-1", "ESU6", AssetClass.FUTURE);
        shortPosition.apply(
                Side.SELL,
                new BigDecimal("200"),
                new BigDecimal("6000"),
                new BigDecimal("1000000"),
                now);
        when(positions.findByAccountIdOrderByInstrumentAsc("ACC-1"))
                .thenReturn(List.of(longPosition, shortPosition));

        var summary = service.riskSummary("ACC-1");

        assertThat(summary.positionCount()).isEqualTo(2);
        assertThat(summary.grossNotional()).isEqualByComparingTo("1218000");
        assertThat(summary.netNotional()).isEqualByComparingTo("-1182000");
        assertThat(summary.breachedPositions()).isOne();
    }

    @Test
    void mapsPositionsForApi() {
        when(positions.findByAccountIdOrderByInstrumentAsc("ACC-1"))
                .thenReturn(List.of(position("25", "200")));

        var result = service.positions("ACC-1");

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.instrument()).isEqualTo("AAPL");
            assertThat(item.netNotional()).isEqualByComparingTo("5000");
        });
    }

    private ExecutionRequest request(String id) {
        return new ExecutionRequest(
                id,
                "ACC-1",
                "AAPL",
                AssetClass.EQUITY,
                Side.BUY,
                new BigDecimal("100"),
                new BigDecimal("180"),
                "USD",
                now.minusSeconds(1));
    }

    private PositionEntity position(String quantity, String price) {
        PositionEntity position = new PositionEntity("ACC-1", "AAPL", AssetClass.EQUITY);
        position.apply(
                Side.BUY,
                new BigDecimal(quantity),
                new BigDecimal(price),
                new BigDecimal("1000000"),
                now);
        return position;
    }
}

