package com.bricopro.user.repository;

import com.bricopro.user.entity.WorkerAvailability;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface WorkerAvailabilityRepository extends JpaRepository<WorkerAvailability, Long> {
    List<WorkerAvailability> findByWorkerProfileIdAndDateBetween(Long workerId, LocalDate from, LocalDate to);
    Optional<WorkerAvailability> findByWorkerProfileIdAndDate(Long workerId, LocalDate date);
    @Query(value = """
        SELECT wa.* FROM worker_availability wa
        JOIN worker_profiles wp ON wp.id = wa.worker_profile_id
        JOIN users u ON u.id = wp.user_id
        WHERE u.status = 'ACTIVE'
        AND wa.date = :date
        AND wa.status = 'AVAILABLE'
        AND wa.start_time <= :start AND (wa.end_time IS NULL OR wa.end_time >= :end)
        AND (6371 * acos(cos(radians(:lat)) * cos(radians(wp.latitude))
            * cos(radians(wp.longitude) - radians(:lng)) + sin(radians(:lat))
            * sin(radians(wp.latitude)))) <= :radius
    """, nativeQuery = true)
    List<WorkerAvailability> findAvailableWorkers(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radius") double radiusKm,
            @Param("date") LocalDate date,
            @Param("start") LocalTime start,
            @Param("end") LocalTime end
    );
}
