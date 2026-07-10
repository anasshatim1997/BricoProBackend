package com.bricopro.auth.controller;

import com.bricopro.auth.dto.AuthDtos;
import com.bricopro.auth.service.OAuth2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/oauth")
@RequiredArgsConstructor
@Tag(name = "OAuth2 Authentication")
public class OAuth2Controller {

    private final OAuth2Service oauth2Service;

    @Operation(summary = "Login with Google ID token")
    @PostMapping("/google")
    public ResponseEntity<AuthDtos.TokenResponse> googleLogin(@Valid @RequestBody AuthDtos.GoogleOAuthRequest request) {
        return ResponseEntity.ok(oauth2Service.googleLogin(request.getIdToken()));
    }

    @Operation(summary = "Login with Facebook access token")
    @PostMapping("/facebook")
    public ResponseEntity<AuthDtos.TokenResponse> facebookLogin(@Valid @RequestBody AuthDtos.FacebookOAuthRequest request) {
        return ResponseEntity.ok(oauth2Service.facebookLogin(request.getAccessToken()));
    }
}