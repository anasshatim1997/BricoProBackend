package com.bricopro.payment.repository;

import com.bricopro.payment.entity.PlatformRevenue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformRevenueRepository extends JpaRepository<PlatformRevenue, Long> {
    boolean existsByPaymentId(Long paymentId);
}
