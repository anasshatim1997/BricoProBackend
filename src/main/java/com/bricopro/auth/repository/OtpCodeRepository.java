package com.bricopro.auth.repository;

import com.bricopro.auth.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {
    Optional<OtpCode> findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(Long userId, OtpCode.Purpose purpose);
}
