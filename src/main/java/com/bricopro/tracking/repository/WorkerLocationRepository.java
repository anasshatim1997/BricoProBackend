package com.bricopro.tracking.repository;

import com.bricopro.tracking.entity.WorkerLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkerLocationRepository extends JpaRepository<WorkerLocation, Long> {
    Optional<WorkerLocation> findByWorkerId(Long workerId);
}
