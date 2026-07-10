package com.bricopro.auth.service;

import com.bricopro.auth.dto.AuthDtos;
import com.bricopro.auth.entity.OAuth2Account;
import com.bricopro.auth.repository.OAuth2AccountRepository;
import com.bricopro.config.propreties.OAuth2Properties;
import com.bricopro.security.jwt.JwtService;
import com.bricopro.security.oauth2.RefreshTokenService;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Role;
import com.bricopro.user.entity.User.Status;
import com.bricopro.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OAuth2Service {

    private final OAuth2AccountRepository oauth2AccountRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final OAuth2Properties oauth2Properties;
    private final WebClient webClient;

    @Transactional
    public AuthDtos.TokenResponse googleLogin(String idToken) {
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
        JsonNode payload = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        if (payload == null) {
            throw new IllegalArgumentException("Invalid Google token response");
        }
        JsonNode audNode = payload.get("aud");
        if (audNode == null || !audNode.asText().equals(oauth2Properties.getGoogle().getClientId())) {
            throw new IllegalArgumentException("Invalid audience");
        }
        JsonNode emailNode = payload.get("email");
        if (emailNode == null) {
            throw new IllegalArgumentException("Email not provided by Google");
        }
        String email = emailNode.asText();
        JsonNode subNode = payload.get("sub");
        if (subNode == null) {
            throw new IllegalArgumentException("User ID not provided by Google");
        }
        String providerUserId = subNode.asText();
        JsonNode givenNameNode = payload.get("given_name");
        String firstName = givenNameNode != null ? givenNameNode.asText() : "";
        JsonNode familyNameNode = payload.get("family_name");
        String lastName = familyNameNode != null ? familyNameNode.asText() : "";
        return processOAuthUser(OAuth2Account.Provider.GOOGLE, providerUserId, email, firstName, lastName);
    }

    @Transactional
    public AuthDtos.TokenResponse facebookLogin(String accessToken) {
        String appId = oauth2Properties.getFacebook().getClientId();
        String appSecret = oauth2Properties.getFacebook().getClientSecret();
        String debugUrl = String.format("https://graph.facebook.com/debug_token?input_token=%s&access_token=%s|%s", accessToken, appId, appSecret);
        JsonNode debug = webClient.get()
                .uri(debugUrl)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        if (debug == null || !debug.path("data").path("is_valid").asBoolean()) {
            throw new IllegalArgumentException("Invalid Facebook token");
        }
        String profileUrl = String.format("https://graph.facebook.com/me?fields=id,name,email,first_name,last_name&access_token=%s", accessToken);
        JsonNode profile = webClient.get()
                .uri(profileUrl)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        if (profile == null) {
            throw new IllegalArgumentException("Invalid Facebook profile response");
        }
        JsonNode idNode = profile.get("id");
        if (idNode == null) {
            throw new IllegalArgumentException("User ID not provided by Facebook");
        }
        String providerUserId = idNode.asText();
        JsonNode emailNode = profile.get("email");
        String email = emailNode != null ? emailNode.asText() : null;
        JsonNode firstNameNode = profile.get("first_name");
        String firstName = firstNameNode != null ? firstNameNode.asText() : "";
        JsonNode lastNameNode = profile.get("last_name");
        String lastName = lastNameNode != null ? lastNameNode.asText() : "";
        return processOAuthUser(OAuth2Account.Provider.FACEBOOK, providerUserId, email, firstName, lastName);
    }

    private AuthDtos.TokenResponse processOAuthUser(OAuth2Account.Provider provider, String providerUserId, String email, String firstName, String lastName) {
        Optional<OAuth2Account> existing = oauth2AccountRepository.findByProviderAndProviderUserId(provider, providerUserId);
        User user;
        if (existing.isPresent()) {
            user = existing.get().getUser();
        } else if (email != null && userRepository.findByEmail(email).isPresent()) {
            user = userRepository.findByEmail(email).get();
            OAuth2Account account = OAuth2Account.builder()
                    .provider(provider)
                    .providerUserId(providerUserId)
                    .user(user)
                    .build();
            oauth2AccountRepository.save(account);
        } else {
            user = User.builder()
                    .firstName(firstName.isEmpty() ? "User" : firstName)
                    .lastName(lastName.isEmpty() ? "User" : lastName)
                    .email(email)
                    .role(Role.CLIENT)
                    .status(Status.ACTIVE)
                    .isVerified(email != null)
                    .build();
            user = userRepository.save(user);
            OAuth2Account account = OAuth2Account.builder()
                    .provider(provider)
                    .providerUserId(providerUserId)
                    .user(user)
                    .build();
            oauth2AccountRepository.save(account);
        }
        if (!user.isVerified() && email != null) {
            user.setVerified(true);
            user.setStatus(Status.ACTIVE);
            userRepository.save(user);
        }
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.create(user).getToken();
        return new AuthDtos.TokenResponse(accessToken, refreshToken, user.getId(), user.getRole().name());
    }
}