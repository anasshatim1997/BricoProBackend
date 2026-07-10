package com.bricopro.user.repository;

import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import com.bricopro.user.entity.User.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WorkerProfileRepository extends JpaRepository<WorkerProfile, Long> {

    Optional<WorkerProfile> findByUserId(Long userId);

    Page<WorkerProfile> findByUserStatus(Status status, Pageable pageable);

    boolean existsByCinNumberAndIdNot(String cinNumber, Long id);

    @Query(value = """
            SELECT wp.* FROM worker_profiles wp
            JOIN users u ON wp.user_id = u.id
            WHERE u.status = 'ACTIVE'
              AND wp.latitude IS NOT NULL
              AND wp.longitude IS NOT NULL
              AND (:serviceType IS NULL OR EXISTS (
                    SELECT 1 FROM worker_services ws
                    WHERE ws.worker_id = wp.id AND ws.service_type = :#{#serviceType?.name()}
              ))
              AND (
                6371 * ACOS(
                  COS(RADIANS(:lat)) * COS(RADIANS(wp.latitude)) *
                  COS(RADIANS(wp.longitude) - RADIANS(:lng)) +
                  SIN(RADIANS(:lat)) * SIN(RADIANS(wp.latitude))
                )
              ) <= :radiusKm
            """,
            countQuery = """
            SELECT COUNT(*) FROM worker_profiles wp
            JOIN users u ON wp.user_id = u.id
            WHERE u.status = 'ACTIVE'
              AND wp.latitude IS NOT NULL
              AND wp.longitude IS NOT NULL
              AND (:serviceType IS NULL OR EXISTS (
                    SELECT 1 FROM worker_services ws
                    WHERE ws.worker_id = wp.id AND ws.service_type = :#{#serviceType?.name()}
              ))
              AND (
                6371 * ACOS(
                  COS(RADIANS(:lat)) * COS(RADIANS(wp.latitude)) *
                  COS(RADIANS(wp.longitude) - RADIANS(:lng)) +
                  SIN(RADIANS(:lat)) * SIN(RADIANS(wp.latitude))
                )
              ) <= :radiusKm
            """,
            nativeQuery = true)
    Page<WorkerProfile> findNearbyWorkers(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm,
            @Param("serviceType") ServiceType serviceType,
            Pageable pageable);

    @Query("SELECT wp FROM WorkerProfile wp WHERE wp.cinVerified = false AND wp.user.status = :status")
    Page<WorkerProfile> findByCinVerifiedFalseAndUserStatus(
            @Param("status") com.bricopro.user.entity.User.Status status, Pageable pageable);

    @Query("SELECT wp FROM WorkerProfile wp JOIN wp.services s " +
            "WHERE (:serviceType IS NULL OR s.serviceType = :serviceType) " +
            "AND (:city IS NULL OR LOWER(wp.city) = LOWER(:city)) " +
            "AND wp.user.status = 'ACTIVE'")
    Page<WorkerProfile> findByFilters(
            @Param("serviceType") ServiceType serviceType,
            @Param("city") String city,
            Pageable pageable);
}