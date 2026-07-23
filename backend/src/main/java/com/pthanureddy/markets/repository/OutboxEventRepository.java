package com.pthanureddy.markets.repository;

import com.pthanureddy.markets.domain.OutboxEventEntity;
import com.pthanureddy.markets.domain.OutboxStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
    List<OutboxEventEntity> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);
    long countByStatus(OutboxStatus status);
}

