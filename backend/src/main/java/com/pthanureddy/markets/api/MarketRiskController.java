package com.pthanureddy.markets.api;

import com.pthanureddy.markets.domain.OutboxStatus;
import com.pthanureddy.markets.repository.OutboxEventRepository;
import com.pthanureddy.markets.service.ExecutionApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class MarketRiskController {
    private final ExecutionApplicationService service;
    private final OutboxEventRepository outbox;

    public MarketRiskController(ExecutionApplicationService service, OutboxEventRepository outbox) {
        this.service = service;
        this.outbox = outbox;
    }

    @PostMapping("/executions")
    @ResponseStatus(HttpStatus.CREATED)
    public ExecutionResponse record(@Valid @RequestBody ExecutionRequest request) {
        return service.record(request);
    }

    @GetMapping("/accounts/{accountId}/positions")
    public List<PositionResponse> positions(
            @PathVariable @NotBlank @Size(max = 80) String accountId) {
        return service.positions(accountId);
    }

    @GetMapping("/accounts/{accountId}/risk")
    public RiskSummaryResponse risk(@PathVariable @NotBlank @Size(max = 80) String accountId) {
        return service.riskSummary(accountId);
    }

    @GetMapping("/outbox/status")
    public OutboxStatusResponse outboxStatus() {
        return new OutboxStatusResponse(
                outbox.countByStatus(OutboxStatus.PENDING),
                outbox.countByStatus(OutboxStatus.PUBLISHED),
                outbox.countByStatus(OutboxStatus.FAILED));
    }
}

