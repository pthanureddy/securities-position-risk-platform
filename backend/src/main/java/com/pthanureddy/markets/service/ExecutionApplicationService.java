package com.pthanureddy.markets.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pthanureddy.markets.api.ExecutionRequest;
import com.pthanureddy.markets.api.ExecutionResponse;
import com.pthanureddy.markets.api.PositionResponse;
import com.pthanureddy.markets.api.RiskSummaryResponse;
import com.pthanureddy.markets.cache.RiskSnapshotCache;
import com.pthanureddy.markets.domain.ExecutionEntity;
import com.pthanureddy.markets.domain.OutboxEventEntity;
import com.pthanureddy.markets.domain.PositionEntity;
import com.pthanureddy.markets.repository.ExecutionRepository;
import com.pthanureddy.markets.repository.OutboxEventRepository;
import com.pthanureddy.markets.repository.PositionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExecutionApplicationService {
    private static final Logger LOG = LoggerFactory.getLogger(ExecutionApplicationService.class);

    private final ExecutionRepository executions;
    private final PositionRepository positions;
    private final OutboxEventRepository outbox;
    private final RiskSnapshotCache cache;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final BigDecimal absoluteNotionalLimit;
    private final Counter acceptedCounter;
    private final Counter duplicateCounter;
    private final Counter breachCounter;

    @Autowired
    public ExecutionApplicationService(
            ExecutionRepository executions,
            PositionRepository positions,
            OutboxEventRepository outbox,
            RiskSnapshotCache cache,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            @Value("${app.risk.absolute-notional-limit:1000000}") BigDecimal absoluteNotionalLimit) {
        this(executions, positions, outbox, cache, objectMapper, meterRegistry, Clock.systemUTC(), absoluteNotionalLimit);
    }

    ExecutionApplicationService(
            ExecutionRepository executions,
            PositionRepository positions,
            OutboxEventRepository outbox,
            RiskSnapshotCache cache,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            Clock clock,
            BigDecimal absoluteNotionalLimit) {
        this.executions = executions;
        this.positions = positions;
        this.outbox = outbox;
        this.cache = cache;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.absoluteNotionalLimit = absoluteNotionalLimit;
        this.acceptedCounter = meterRegistry.counter("market.executions.accepted");
        this.duplicateCounter = meterRegistry.counter("market.executions.duplicate");
        this.breachCounter = meterRegistry.counter("market.risk.limit_breach");
    }

    @Transactional
    public ExecutionResponse record(ExecutionRequest request) {
        var existing = executions.findById(request.executionId());
        if (existing.isPresent()) {
            duplicateCounter.increment();
            PositionEntity position = positions.findForUpdate(request.accountId(), request.instrument())
                    .orElseThrow(() -> new IllegalStateException("Execution exists without a position"));
            return response(existing.get().getExecutionId(), false, true, position);
        }

        Instant now = clock.instant();
        ExecutionEntity execution = new ExecutionEntity(
                request.executionId(),
                request.accountId(),
                request.instrument(),
                request.assetClass(),
                request.side(),
                request.quantity(),
                request.price(),
                request.currency(),
                request.executedAt(),
                now);

        PositionEntity position = positions.findForUpdate(request.accountId(), request.instrument())
                .orElseGet(() -> new PositionEntity(request.accountId(), request.instrument(), request.assetClass()));
        position.apply(request.side(), request.quantity(), request.price(), absoluteNotionalLimit, now);

        executions.save(execution);
        positions.save(position);
        outbox.save(new OutboxEventEntity(
                UUID.randomUUID(),
                request.executionId(),
                "ExecutionAccepted.v1",
                eventPayload(execution, position),
                now));

        acceptedCounter.increment();
        if (position.isLimitBreached()) {
            breachCounter.increment();
        }

        RiskSummaryResponse summary =
                summarize(request.accountId(), positions.findByAccountIdOrderByInstrumentAsc(request.accountId()));
        try {
            cache.put(summary);
        } catch (RuntimeException cacheFailure) {
            LOG.warn("Risk snapshot cache unavailable for account {}", request.accountId(), cacheFailure);
        }
        return response(request.executionId(), true, false, position);
    }

    @Transactional(readOnly = true)
    public List<PositionResponse> positions(String accountId) {
        return positions.findByAccountIdOrderByInstrumentAsc(accountId).stream()
                .map(this::positionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RiskSummaryResponse riskSummary(String accountId) {
        return summarize(accountId, positions.findByAccountIdOrderByInstrumentAsc(accountId));
    }

    private RiskSummaryResponse summarize(String accountId, List<PositionEntity> accountPositions) {
        BigDecimal gross = accountPositions.stream()
                .map(PositionEntity::getNetNotional)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal net = accountPositions.stream()
                .map(PositionEntity::getNetNotional)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long breached = accountPositions.stream().filter(PositionEntity::isLimitBreached).count();
        return new RiskSummaryResponse(accountId, accountPositions.size(), breached, gross, net);
    }

    private ExecutionResponse response(String executionId, boolean accepted, boolean duplicate, PositionEntity position) {
        return new ExecutionResponse(
                executionId,
                accepted,
                duplicate,
                position.getAccountId(),
                position.getInstrument(),
                position.getSignedQuantity(),
                position.getNetNotional(),
                position.isLimitBreached());
    }

    private PositionResponse positionResponse(PositionEntity position) {
        return new PositionResponse(
                position.getAccountId(),
                position.getInstrument(),
                position.getAssetClass(),
                position.getSignedQuantity(),
                position.getLastPrice(),
                position.getNetNotional(),
                position.isLimitBreached(),
                position.getUpdatedAt());
    }

    private String eventPayload(ExecutionEntity execution, PositionEntity position) {
        try {
            return objectMapper.writeValueAsString(Map.ofEntries(
                    Map.entry("schemaVersion", 1),
                    Map.entry("executionId", execution.getExecutionId()),
                    Map.entry("accountId", execution.getAccountId()),
                    Map.entry("instrument", execution.getInstrument()),
                    Map.entry("assetClass", execution.getAssetClass()),
                    Map.entry("side", execution.getSide()),
                    Map.entry("quantity", execution.getQuantity()),
                    Map.entry("price", execution.getPrice()),
                    Map.entry("currency", execution.getCurrency()),
                    Map.entry("executedAt", execution.getExecutedAt()),
                    Map.entry("signedQuantity", position.getSignedQuantity()),
                    Map.entry("netNotional", position.getNetNotional()),
                    Map.entry("limitBreached", position.isLimitBreached())));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize outbox event", exception);
        }
    }
}
