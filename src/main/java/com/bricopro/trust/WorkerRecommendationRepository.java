package com.bricopro.trust;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkerRecommendationRepository extends JpaRepository<WorkerRecommendation, Long> {
    long countByWorkerId(Long workerId);
    boolean existsByRecommenderIdAndWorkerId(Long recommenderId, Long workerId);
    List<WorkerRecommendation> findByWorkerId(Long workerId);

    @Query("SELECT r FROM WorkerRecommendation r WHERE r.worker.id = :workerId " +
            "AND r.recommender.id IN (SELECT f.worker.id FROM ClientFavorite f WHERE f.client.id = :viewerId)")
    List<WorkerRecommendation> findNetworkRecommendations(
            @Param("workerId") Long workerId,
            @Param("viewerId") Long viewerId
    );
}