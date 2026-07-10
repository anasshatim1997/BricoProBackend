package com.bricopro.trust;

import com.bricopro.user.entity.User;
import com.bricopro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkerTrustService {

    private final WorkerRecommendationRepository recommendationRepository;
    private final UserRepository userRepository;

    @Transactional
    public WorkerRecommendation recommend(Long recommenderId, Long workerId, String note) {
        if (recommenderId.equals(workerId))
            throw new IllegalArgumentException("Cannot recommend yourself");
        if (recommendationRepository.existsByRecommenderIdAndWorkerId(recommenderId, workerId))
            throw new IllegalStateException("Already recommended this worker");

        User recommender = userRepository.findById(recommenderId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found"));

        return recommendationRepository.save(WorkerRecommendation.builder()
                .recommender(recommender).worker(worker).note(note).build());
    }

    public Map<String, Object> getWorkerTrustScore(Long workerId, Long viewerId) {
        long totalRecs = recommendationRepository.countByWorkerId(workerId);
        List<WorkerRecommendation> networkRecs = viewerId != null
                ? recommendationRepository.findNetworkRecommendations(workerId, viewerId)
                : List.of();

        return Map.of(
                "totalRecommendations", totalRecs,
                "networkRecommendations", networkRecs.size(),
                "trustedByNetwork", !networkRecs.isEmpty(),
                "trustLevel", totalRecs >= 10 ? "HIGH" : totalRecs >= 3 ? "MEDIUM" : "NEW"
        );
    }
}
