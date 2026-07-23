package com.pthanureddy.markets.repository;

import com.pthanureddy.markets.domain.ExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionRepository extends JpaRepository<ExecutionEntity, String> {}

