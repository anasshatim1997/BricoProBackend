package com.bricopro.user.repository;

import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Role;
import com.bricopro.user.entity.User.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    List<User> findByRole(Role role);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'WORKER' AND u.status = 'ACTIVE'")
    long countActiveWorkers();
    @Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:status IS NULL OR u.status = :status) AND " +
           "(:search IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%',:search,'%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%',:search,'%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%',:search,'%')))")
    Page<User> search(Role role, Status status, String search, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = 'WORKER' AND (LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<User> findWorkersByNameContaining(@Param("q") String q);


    @Query("SELECT COUNT(wp) FROM WorkerProfile wp WHERE wp.city = :city AND wp.user.status = 'ACTIVE'")
    Long countActiveWorkersInCity(@Param("city") String city);


}
