package com.bricopro.booking.repository;

import com.bricopro.booking.entity.GroupBookingWorker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupBookingWorkerRepository extends JpaRepository<GroupBookingWorker, Long> {
    List<GroupBookingWorker> findByGroupBookingId(Long groupBookingId);
    boolean existsByGroupBookingIdAndWorkerId(Long groupBookingId, Long workerId);
}
