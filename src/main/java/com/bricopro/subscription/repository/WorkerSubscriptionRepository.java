package com.bricopro.subscription.repository;

import com.bricopro.subscription.entity.WorkerSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkerSubscriptionRepository extends JpaRepository<WorkerSubscription, Long> {
    Optional<WorkerSubscription> findTopByWorkerIdAndSubStatusOrderByCreatedAtDesc(
            Long workerId, WorkerSubscription.SubStatus status);
}
