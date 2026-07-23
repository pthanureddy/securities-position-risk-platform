# Operations Runbook

## Signals

| Signal | Meaning | First response |
| --- | --- | --- |
| `/actuator/health/readiness` not `UP` | Pod should not receive traffic | Inspect database connectivity and configuration |
| `market.executions.accepted` stops increasing | Intake is idle or failing | Compare API error rate and upstream traffic |
| `market.executions.duplicate` increases | Client retries or replay | Confirm repeated IDs are expected |
| `market.risk.limit_breach` increases | Position crossed local notional limit | Inspect account positions; this is not an automated trading decision |
| Outbox pending count increases | Kafka is slow or unavailable | Check broker connectivity and publisher logs |
| Outbox failed count is non-zero | Retry budget exhausted | Preserve event, diagnose broker/authentication, then design controlled replay |

## Kafka outage

1. Confirm the API and PostgreSQL remain healthy.
2. Query `GET /api/v1/outbox/status`.
3. Inspect broker DNS, network policy, authentication, and topic availability.
4. Do not delete outbox records.
5. Restore the dependency and verify pending count declines.
6. Failed events need an explicit operator-reviewed replay procedure; the
   portfolio version intentionally does not mutate them automatically.

## Redis outage

The API logs the cache failure and continues. PostgreSQL queries remain the
source of truth. Restore Redis, then submit or recompute a risk view to refresh
the short-lived snapshot. Never treat cached data as the authoritative ledger.

## Database contention

Position changes for the same account and instrument use a pessimistic row lock.
Investigate slow transaction metrics and database lock waits before increasing
timeouts. Keep the transaction free of broker calls; Kafka publication is
performed separately through the outbox.

## Rollback

Use an immutable container tag, keep the database migration contract backward
compatible, and roll the deployment back through Kubernetes. This portfolio
version uses Hibernate schema updates for local convenience; a production
implementation should replace that with reviewed Flyway migrations.

## Data handling

Repository examples are synthetic. Logs should contain identifiers and failure
context, not credentials. Kubernetes secrets are referenced by name and are not
committed. Rotate credentials through the deployment platform.

