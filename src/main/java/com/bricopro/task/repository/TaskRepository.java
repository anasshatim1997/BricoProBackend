package com.bricopro.task.repository;

import com.bricopro.task.entity.Task;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @EntityGraph(attributePaths = {"client", "worker", "photos"})
    Optional<Task> findById(Long id);

    @EntityGraph(attributePaths = {"client", "worker", "photos"})
    Page<Task> findByClientId(Long clientId, Pageable pageable);

    @EntityGraph(attributePaths = {"client", "worker", "photos"})
    Page<Task> findByClientIdAndStatus(Long clientId, TaskStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"client", "worker", "photos"})
    Page<Task> findByWorkerId(Long workerId, Pageable pageable);

    @EntityGraph(attributePaths = {"client", "worker", "photos"})
    Page<Task> findByWorkerIdAndStatus(Long workerId, TaskStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"client", "worker", "photos"})
    @Query("""
        SELECT t FROM Task t
        WHERE t.status = 'SEARCHING'
        AND (:serviceType IS NULL OR t.serviceType = :serviceType)
    """)
    Page<Task> findAvailableForWorker(
            @Param("serviceType") ServiceType serviceType,
            Pageable pageable
    );

    Page<Task> findByStatus(TaskStatus status, Pageable pageable);
    List<Task> findByStatus(TaskStatus status);

    @Query("SELECT t.serviceType, COUNT(t) FROM Task t WHERE t.status = :status GROUP BY t.serviceType")
    List<Object[]> countByStatusGroupedByServiceType(@Param("status") TaskStatus status);
    Page<Task> findByServiceType(ServiceType serviceType, Pageable pageable);
    Page<Task> findByStatusAndServiceType(TaskStatus status, ServiceType serviceType, Pageable pageable);

    long countByStatus(TaskStatus status);
    long countByClientId(Long clientId);
    long countByWorkerId(Long workerId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.serviceType = :serviceType")
    long countByServiceType(@Param("serviceType") ServiceType serviceType);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.worker.id = :workerId AND t.status = :status")
    long countByWorkerIdAndStatus(@Param("workerId") Long workerId, @Param("status") TaskStatus status);

    boolean existsByClientIdAndWorkerIdAndStatusIn(Long clientId, Long workerId, List<TaskStatus> statuses);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.client.id = :clientId AND t.status = :status")
    long countByClientIdAndStatus(@Param("clientId") Long clientId, @Param("status") TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.status IN ('CONFIRMED', 'STARTED')")
    long countActiveTasks();

    @Modifying
    @Query("""
    UPDATE Task t
    SET t.worker = (SELECT u FROM User u WHERE u.id = :workerId),
        t.status = :newStatus
    WHERE t.id     = :taskId
      AND t.status = :expectedStatus
      AND t.worker IS NULL
    """)
    int claimTask(
            @Param("taskId") Long taskId,
            @Param("workerId") Long workerId,
            @Param("expectedStatus") TaskStatus expectedStatus,
            @Param("newStatus") TaskStatus newStatus
    );

    @Query("""
    SELECT t.serviceType, COUNT(t)
    FROM Task t
    WHERE t.client.id IN (
        SELECT cp.user.id FROM ClientProfile cp WHERE cp.city = :city
    )
    AND t.status = 'COMPLETED'
    GROUP BY t.serviceType
    ORDER BY COUNT(t) DESC
    """)
    List<Object[]> findTrendingServicesByCity(@Param("city") String city, Pageable pageable);

    @Query(value = """
    SELECT AVG(TIMESTAMPDIFF(MINUTE, t.created_at, t.updated_at))
    FROM task t
    WHERE t.client_id IN (
        SELECT cp.user_id FROM client_profile cp WHERE cp.city = :city
    )
    AND t.status = 'COMPLETED'
    AND t.worker_id IS NOT NULL
""", nativeQuery = true)
    Double averageResponseTimeInCity(@Param("city") String city);

    List<Task> findByTitleContainingIgnoreCaseAndClient(String title, User client);

    @Query("SELECT t.serviceType, COUNT(t) FROM Task t WHERE t.client = :client AND t.status = 'COMPLETED' GROUP BY t.serviceType ORDER BY COUNT(t) DESC")
    List<Object[]> countServiceTypesForClient(@Param("client") User client);

    @Query("SELECT t FROM Task t WHERE t.client = :client AND t.status = :status ORDER BY t.scheduledDate DESC")
    List<Task> findTop10ByClientAndStatusOrderByScheduledDateDesc(@Param("client") User client, @Param("status") TaskStatus status, Pageable pageable);
}