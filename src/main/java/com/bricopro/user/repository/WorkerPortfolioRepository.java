package com.bricopro.user.repository;

import com.bricopro.user.entity.WorkerPortfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkerPortfolioRepository extends JpaRepository<WorkerPortfolio, Long> {
    List<WorkerPortfolio> findByWorkerProfileIdOrderByPhotoOrderAsc(Long workerId);
    long countByWorkerProfileId(Long workerId);
}
