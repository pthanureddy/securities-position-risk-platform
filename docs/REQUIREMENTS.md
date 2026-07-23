# Requirements and Verification

| ID | Requirement | Implementation | Verification |
| --- | --- | --- | --- |
| EXE-01 | Accept validated synthetic equity and futures executions | `ExecutionRequest`, `MarketRiskController` | API integration tests |
| CON-01 | Do not apply one execution ID twice | Execution repository lookup and primary key | Replay integration and service tests |
| CON-02 | Avoid lost position updates | Pessimistic position lock and entity version | Repository query, calculation tests |
| CON-03 | Commit state and delivery intent together | Transactional application service and outbox entity | Integration test verifies outbox |
| RSK-01 | Calculate signed quantity and net notional | `PositionEntity.apply` | Five domain tests |
| RSK-02 | Flag absolute notional above configurable limit | Position domain model | Boundary and short-position tests |
| MSG-01 | Retry broker failure without dropping event | `KafkaOutboxPublisher` | Three publisher tests |
| CCH-01 | Do not reject execution when Redis cache is unavailable | guarded `RiskSnapshotCache` call | service failure-path test |
| OBS-01 | Expose health and Prometheus metrics | Actuator, Micrometer configuration | context and API integration tests |
| UI-01 | Present positions, exposure, and limit state | React dashboard | component and calculation tests |
| OPS-01 | Package repeatable local and Kubernetes runtime | Dockerfiles, Compose, Kubernetes manifests | CI container builds and manifest checks |
| CLD-01 | Provide an AWS deployment blueprint | EKS, RDS, MSK Serverless, ElastiCache Terraform | Terraform format and validation in CI |

