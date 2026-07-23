package com.pthanureddy.markets.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pthanureddy.markets.repository.ExecutionRepository;
import com.pthanureddy.markets.repository.OutboxEventRepository;
import com.pthanureddy.markets.repository.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MarketRiskApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ExecutionRepository executions;
    @Autowired PositionRepository positions;
    @Autowired OutboxEventRepository outbox;

    @BeforeEach
    void resetDatabase() {
        outbox.deleteAll();
        executions.deleteAll();
        positions.deleteAll();
    }

    @Test
    void acceptsExecutionAndExposesPositionRiskAndOutbox() throws Exception {
        mvc.perform(post("/api/v1/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(executionJson("EXEC-API-1", "BUY", "100", "180")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.netNotional").value(18000));

        mvc.perform(get("/api/v1/accounts/ACC-API/positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].instrument").value("AAPL"))
                .andExpect(jsonPath("$[0].signedQuantity").value(100));

        mvc.perform(get("/api/v1/accounts/ACC-API/risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positionCount").value(1))
                .andExpect(jsonPath("$.grossNotional").value(18000));

        mvc.perform(get("/api/v1/outbox/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending").value(1))
                .andExpect(jsonPath("$.failed").value(0));
    }

    @Test
    void replayedExecutionIsIdempotent() throws Exception {
        String request = executionJson("EXEC-REPLAY-1", "BUY", "100", "180");
        mvc.perform(post("/api/v1/executions").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/executions").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accepted").value(false))
                .andExpect(jsonPath("$.duplicate").value(true))
                .andExpect(jsonPath("$.signedQuantity").value(100));

        mvc.perform(get("/api/v1/accounts/ACC-API/positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].signedQuantity").value(100));
    }

    @Test
    void rejectsInvalidExecutionBeforePersistence() throws Exception {
        mvc.perform(post("/api/v1/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(executionJson("EXEC-BAD", "BUY", "0", "180")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());

        mvc.perform(get("/api/v1/outbox/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending").value(0));
    }

    @Test
    void calculatesLimitBreachForLargeShortPosition() throws Exception {
        mvc.perform(post("/api/v1/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(executionJson("EXEC-BREACH", "SELL", "6000", "200")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.limitBreached").value(true))
                .andExpect(jsonPath("$.netNotional").value(-1200000));

        mvc.perform(get("/api/v1/accounts/ACC-API/risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.breachedPositions").value(1))
                .andExpect(jsonPath("$.grossNotional").value(1200000));
    }

    @Test
    void returnsEmptySummaryForUnknownAccount() throws Exception {
        mvc.perform(get("/api/v1/accounts/UNKNOWN/risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("UNKNOWN"))
                .andExpect(jsonPath("$.positionCount").value(0))
                .andExpect(jsonPath("$.grossNotional").value(0));
    }

    private String executionJson(String executionId, String side, String quantity, String price) {
        return """
                {
                  "executionId": "%s",
                  "accountId": "ACC-API",
                  "instrument": "AAPL",
                  "assetClass": "EQUITY",
                  "side": "%s",
                  "quantity": %s,
                  "price": %s,
                  "currency": "USD",
                  "executedAt": "2026-07-23T10:00:00Z"
                }
                """.formatted(executionId, side, quantity, price);
    }
}
