package com.bricopro.security.oauth2;

import com.bricopro.auth.entity.RefreshToken;
import com.bricopro.auth.repository.RefreshTokenRepository;
import com.bricopro.config.JwtProperties;
import com.bricopro.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Refresh Token Service", description = "Business logic for Refresh Token Service")
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    @Transactional
    /**
     * Create.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public RefreshToken create(User user) {
        refreshTokenRepository.revokeAllByUserId(user.getId());
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(LocalDateTime.now().plusSeconds(jwtProperties.getRefreshTokenExpirationMs() / 1000));
        return refreshTokenRepository.save(token);
    }

    @Transactional
    /**
     * Find Valid.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public Optional<RefreshToken> findValid(String token) {
        return refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .filter(rt -> rt.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Transactional
    /**
     * Revoke.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public void revoke(String token) {
        refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }
}
