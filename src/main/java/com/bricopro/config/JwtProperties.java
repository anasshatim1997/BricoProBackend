package com.bricopro.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    @Schema(description = "Secret", example = "example")
    private String secret;
    private long accessTokenExpirationMs = 900_000L;
    private long refreshTokenExpirationMs = 604_800_000L;
}
