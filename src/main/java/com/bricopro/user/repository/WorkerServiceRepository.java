package com.bricopro.user.repository;

import com.bricopro.user.entity.WorkerService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkerServiceRepository extends JpaRepository<WorkerService, Long> {

    @Modifying
    @Query("DELETE FROM WorkerService ws WHERE ws.workerProfile.id = :profileId")
    void deleteAllByWorkerProfileId(@Param("profileId") Long profileId);
}
