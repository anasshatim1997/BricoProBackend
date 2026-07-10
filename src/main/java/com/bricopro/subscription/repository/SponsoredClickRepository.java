package com.bricopro.subscription.repository;

import com.bricopro.subscription.entity.SponsoredClick;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SponsoredClickRepository extends JpaRepository<SponsoredClick, Long> {
    boolean existsBySponsoredWorkerIdAndViewerId(Long sponsoredWorkerId, Long viewerId);
}
