package com.bricopro.user.repository;

import com.bricopro.user.entity.WorkerSnapshotHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkerSnapshotHistoryRepository extends JpaRepository<WorkerSnapshotHistory, Long> {
    List<WorkerSnapshotHistory> findByTaskId(Long taskId);
    List<WorkerSnapshotHistory> findByWorkerId(Long workerId);
}
