package com.pthanureddy.markets.repository;

import com.pthanureddy.markets.domain.PositionEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PositionRepository extends JpaRepository<PositionEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PositionEntity p where p.accountId = :accountId and p.instrument = :instrument")
    Optional<PositionEntity> findForUpdate(
            @Param("accountId") String accountId, @Param("instrument") String instrument);

    List<PositionEntity> findByAccountIdOrderByInstrumentAsc(String accountId);
}

