# Performance and Resilience Test Strategy

No throughput or latency number is claimed because this repository has not been
benchmarked on controlled production-equivalent infrastructure.

## Reproducible baseline design

A meaningful load test should:

- use a fixed synthetic execution set with at least 80% unique positions and a
  deliberate hot-account subset;
- report p50, p95, and p99 latency separately for new and replayed execution IDs;
- observe PostgreSQL lock waits, connection-pool saturation, JVM allocation,
  Kafka acknowledgement latency, outbox depth, and Redis failures;
- run at several concurrency levels until a documented saturation point;
- repeat each level and publish raw results plus hardware and container limits;
- verify invariants after the run: one execution row per ID, expected signed
  quantity, no missing outbox record, and no duplicate publication key.

## Failure experiments

1. Stop Kafka while continuing execution intake. Accepted writes should persist
   and outbox depth should rise.
2. Restore Kafka. Pending depth should fall without reapplying positions.
3. Stop Redis. Intake should continue and authoritative risk queries should
   remain correct.
4. Retry the same execution ID concurrently. Exactly one position delta should
   be visible.
5. Send two different executions for one hot position. Locking should serialize
   updates without a lost write.

## Performance decisions already visible in code

- Account/instrument position lookups use a database key and row lock.
- The publisher uses bounded batches of 50.
- Redis stores derived summaries for five minutes.
- Broker publication is outside the execution transaction.
- Prometheus metrics expose intake, replay, and breach counts.

