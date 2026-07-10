package com.bricopro.badge.repository;

import com.bricopro.badge.entity.WorkerBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkerBadgeRepository extends JpaRepository<WorkerBadge, Long> {
    List<WorkerBadge> findByUserId(Long userId);
    boolean existsByUserIdAndBadgeType(Long userId, WorkerBadge.BadgeType type);
}
