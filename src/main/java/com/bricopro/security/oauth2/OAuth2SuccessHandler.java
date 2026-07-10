package com.bricopro.security.oauth2;

import com.bricopro.auth.entity.OAuth2Account;
import com.bricopro.auth.repository.OAuth2AccountRepository;
import com.bricopro.security.jwt.JwtService;
import com.bricopro.security.oauth2.OAuth2ExchangeCodeService.ExchangePayload;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Role;
import com.bricopro.user.entity.User.Status;
import com.bricopro.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository          userRepository;
    private final OAuth2AccountRepository oAuth2AccountRepository;
    private final JwtService              jwtService;
    private final RefreshTokenService     refreshTokenService;
    private final OAuth2ExchangeCodeService exchangeCodeService;

    @Value("${app.oauth2.redirect-uri:brico://oauth2/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauthUser = oauthToken.getPrincipal();
        String registrationId = oauthToken.getAuthorizedClientRegistrationId().toUpperCase();

        String providerUserId = oauthUser.getAttribute("sub") != null
                ? oauthUser.getAttribute("sub")
                : oauthUser.getAttribute("id");

        String email     = oauthUser.getAttribute("email");
        String firstName = oauthUser.getAttribute("given_name")  != null
                ? oauthUser.getAttribute("given_name")  : oauthUser.getAttribute("first_name");
        String lastName  = oauthUser.getAttribute("family_name") != null
                ? oauthUser.getAttribute("family_name") : oauthUser.getAttribute("last_name");
        String picture   = oauthUser.getAttribute("picture");

        OAuth2Account.Provider provider = OAuth2Account.Provider.valueOf(registrationId);

        User user = oAuth2AccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(OAuth2Account::getUser)
                .orElseGet(() -> {
                    User newUser = userRepository.findByEmail(email).orElseGet(() -> {
                        User u = new User();
                        u.setEmail(email);
                        u.setFirstName(firstName != null ? firstName : "");
                        u.setLastName(lastName  != null ? lastName  : "");
                        u.setAvatarUrl(picture);
                        u.setRole(Role.CLIENT);
                        u.setStatus(Status.ACTIVE);
                        u.setVerified(true);
                        return userRepository.save(u);
                    });

                    OAuth2Account account = new OAuth2Account();
                    account.setUser(newUser);
                    account.setProvider(provider);
                    account.setProviderUserId(providerUserId);
                    oAuth2AccountRepository.save(account);
                    return newUser;
                });

        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.create(user).getToken();

        String code = exchangeCodeService.store(new ExchangePayload(
                accessToken, refreshToken, user.getId(), user.getRole().name()));

        String callbackUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("code", code)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, callbackUrl);
    }
}
