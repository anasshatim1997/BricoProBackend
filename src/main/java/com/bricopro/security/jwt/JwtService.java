package com.bricopro.security.jwt;

import com.bricopro.config.JwtProperties;
import com.bricopro.user.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Jwt Service", description = "Business logic for Jwt Service")
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties props;

    /**
     * Generate Access Token.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .claim("email", user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + props.getAccessTokenExpirationMs()))
                .signWith(signingKey())
                .compact();
    }

    /**
     * Parse Token.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Is Valid.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Extract User Id.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public Long extractUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    /**
     * Extract Role.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public String extractRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.getSecret()));
    }
}
