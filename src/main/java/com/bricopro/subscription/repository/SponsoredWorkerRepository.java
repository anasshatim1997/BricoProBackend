package com.bricopro.subscription.repository;

import com.bricopro.subscription.entity.SponsoredWorker;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SponsoredWorkerRepository extends JpaRepository<SponsoredWorker, Long> {
    @Query("SELECT s FROM SponsoredWorker s WHERE s.active = true " +
           "AND (:serviceType IS NULL OR s.serviceType = :serviceType) " +
           "AND (:city IS NULL OR LOWER(s.targetCity) = LOWER(:city)) " +
           "AND s.endsAt > CURRENT_TIMESTAMP " +
           "AND (s.spent IS NULL OR s.spent < s.dailyBudget) " +
           "ORDER BY s.costPerClick DESC")
    List<SponsoredWorker> findActiveSponsoredWorkers(ServiceType serviceType, String city);

    List<SponsoredWorker> findByWorkerId(Long workerId);
}
