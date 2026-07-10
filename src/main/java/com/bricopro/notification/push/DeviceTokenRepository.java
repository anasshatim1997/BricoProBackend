package com.bricopro.notification.push;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// ─────────────────────────────────────────────────────────────────────────────
// Repository
// ─────────────────────────────────────────────────────────────────────────────
interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    List<DeviceToken> findByUserId(Long userId);

    void deleteByUserIdAndDeviceToken(Long userId, String token);

    boolean existsByUserIdAndDeviceToken(Long userId, String token);
}
