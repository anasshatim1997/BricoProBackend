package com.bricopro.user.service;

import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.entity.WorkerSnapshotHistory;
import com.bricopro.user.repository.WorkerProfileRepository;
import com.bricopro.user.repository.WorkerSnapshotHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkerSnapshotService {

    private final WorkerProfileRepository workerProfileRepository;
    private final WorkerSnapshotHistoryRepository snapshotRepository;

    @Transactional
    public void captureOnAssignment(Long workerId, Long taskId) {
        WorkerProfile profile = workerProfileRepository.findByUserId(workerId).orElse(null);
        if (profile == null) {
            log.warn("Skipped worker snapshot: no WorkerProfile found for workerId={}", workerId);
            return;
        }

        snapshotRepository.save(WorkerSnapshotHistory.builder()
                .workerId(workerId)
                .taskId(taskId)
                .averageRating(profile.getAverageRating())
                .totalReviews(profile.getTotalReviews())
                .totalMissions(profile.getTotalMissions())
                .responseRate(profile.getResponseRate())
                .cancellationCount(profile.getCancellationCount())
                .isPremium(profile.isPremium())
                .build());
    }
}
