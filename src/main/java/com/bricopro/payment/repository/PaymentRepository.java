package com.bricopro.payment.repository;

import com.bricopro.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTaskId(Long taskId);

    Optional<Payment> findByGatewayReference(String gatewayReference);

    Page<Payment> findByClientId(Long clientId, Pageable pageable);

    Page<Payment> findByWorkerId(Long workerId, Pageable pageable);

@Query("SELECT SUM(p.platformFee) FROM Payment p WHERE p.status = 'COMPLETED'")
    BigDecimal sumAllPlatformFees();

    @Query("SELECT SUM(p.platformFee) FROM Payment p " +
            "WHERE p.status = 'COMPLETED' " +
            "AND MONTH(p.paidAt) = :month AND YEAR(p.paidAt) = :year")
    BigDecimal sumPlatformFeeByMonthAndYear(@Param("month") int month, @Param("year") int year);

@Query("SELECT SUM(p.netAmount) FROM Payment p " +
            "WHERE p.worker.id = :workerId AND p.status = 'COMPLETED'")
    BigDecimal sumAllWorkerRevenue(@Param("workerId") Long workerId);

    @Query("SELECT SUM(p.netAmount) FROM Payment p " +
            "WHERE p.worker.id = :workerId AND p.status = 'COMPLETED' " +
            "AND MONTH(p.paidAt) = :month AND YEAR(p.paidAt) = :year")
    BigDecimal sumWorkerRevenueByMonth(@Param("workerId") Long workerId,
                                       @Param("month") int month,
                                       @Param("year") int year);

@Query("SELECT SUM(p.grossAmount) FROM Payment p " +
            "WHERE p.client.id = :clientId AND p.status = 'COMPLETED'")
    BigDecimal sumAllClientSpent(@Param("clientId") Long clientId);

    @Query("SELECT SUM(p.grossAmount) FROM Payment p " +
            "WHERE p.client.id = :clientId AND p.status = 'COMPLETED' " +
            "AND MONTH(p.paidAt) = :month AND YEAR(p.paidAt) = :year")
    BigDecimal sumClientSpentByMonth(@Param("clientId") Long clientId,
                                     @Param("month") int month,
                                     @Param("year") int year);
}