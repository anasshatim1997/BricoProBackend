package com.bricopro.auth;

import com.bricopro.auth.dto.AuthDtos;
import com.bricopro.auth.entity.OAuth2Account;
import com.bricopro.auth.repository.OAuth2AccountRepository;
import com.bricopro.auth.service.OAuth2Service;
import com.bricopro.config.propreties.OAuth2Properties;
import com.bricopro.auth.entity.RefreshToken;
import com.bricopro.security.jwt.JwtService;
import com.bricopro.security.oauth2.RefreshTokenService;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Role;
import com.bricopro.user.entity.User.Status;
import com.bricopro.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2Service")
class OAuth2ServiceTest {

    @Mock OAuth2AccountRepository oauth2AccountRepository;
    @Mock UserRepository userRepository;
    @Mock JwtService jwtService;
    @Mock RefreshTokenService refreshTokenService;
    @Mock OAuth2Properties oauth2Properties;
    @Mock WebClient webClient;
    @Mock WebClient.RequestHeadersUriSpec uriSpec;
    @Mock WebClient.RequestHeadersSpec headersSpec;
    @Mock WebClient.ResponseSpec responseSpec;

    @InjectMocks OAuth2Service oauth2Service;

    private final ObjectMapper mapper = new ObjectMapper();

    private OAuth2Properties.Google google;
    private OAuth2Properties.Facebook facebook;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        google = new OAuth2Properties.Google();
        google.setClientId("real-google-client-id");
        facebook = new OAuth2Properties.Facebook();
        facebook.setClientId("fb-app-id");
        facebook.setClientSecret("fb-app-secret");

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
    }

    private JsonNode json(String json) throws Exception {
        return mapper.readTree(json);
    }

    @Nested
    @DisplayName("googleLogin()")
    class GoogleLogin {

        @Test
        @DisplayName("rejects a token whose audience doesn't match our client ID")
        void rejectsWrongAudience() throws Exception {
            when(oauth2Properties.getGoogle()).thenReturn(google);
            JsonNode payload = json("""
                {"aud":"some-other-app-client-id","email":"a@test.com","sub":"g-123"}
            """);
            when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(payload));

            assertThatThrownBy(() -> oauth2Service.googleLogin("fake-id-token"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("audience");
        }

        @Test
        @DisplayName("rejects when Google returns no response at all")
        void rejectsNullResponse() {
            when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.empty());

            assertThatThrownBy(() -> oauth2Service.googleLogin("fake-id-token"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects when Google's response has no email field")
        void rejectsMissingEmail() throws Exception {
            when(oauth2Properties.getGoogle()).thenReturn(google);
            JsonNode payload = json("""
                {"aud":"real-google-client-id","sub":"g-123"}
            """);
            when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(payload));

            assertThatThrownBy(() -> oauth2Service.googleLogin("fake-id-token"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email");
        }

        @Test
        @DisplayName("creates a brand-new user on first-ever Google login")
        void createsNewUserOnFirstLogin() throws Exception {
            when(oauth2Properties.getGoogle()).thenReturn(google);
            JsonNode payload = json("""
                {"aud":"real-google-client-id","email":"nouveau@test.ma","sub":"g-999",
                 "given_name":"Sara","family_name":"Tazi"}
            """);
            when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(payload));

            when(oauth2AccountRepository.findByProviderAndProviderUserId(OAuth2Account.Provider.GOOGLE, "g-999"))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail("nouveau@test.ma")).thenReturn(Optional.empty());
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(42L);
                return u;
            });
            when(jwtService.generateAccessToken(any())).thenReturn("access-tok");
            when(refreshTokenService.create(any())).thenReturn(
                    RefreshToken.builder().token("refresh-tok").build());

            AuthDtos.TokenResponse result = oauth2Service.googleLogin("fake-id-token");

            assertThat(result.getAccessToken()).isEqualTo("access-tok");
            assertThat(result.getUserId()).isEqualTo(42L);
            assertThat(result.getRole()).isEqualTo("CLIENT");

            verify(userRepository).save(argThat(u ->
                    u.getFirstName().equals("Sara") && u.getEmail().equals("nouveau@test.ma")
                            && u.getRole() == Role.CLIENT && u.isVerified()));
            verify(oauth2AccountRepository).save(argThat(acc ->
                    acc.getProvider() == OAuth2Account.Provider.GOOGLE
                            && acc.getProviderUserId().equals("g-999")));
        }

        @Test
        @DisplayName("links Google to an existing account found by email, rather than creating a duplicate")
        void linksToExistingAccountByEmail() throws Exception {
            when(oauth2Properties.getGoogle()).thenReturn(google);
            JsonNode payload = json("""
                {"aud":"real-google-client-id","email":"existe@test.ma","sub":"g-777"}
            """);
            when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(payload));

            User existingUser = User.builder().id(7L).email("existe@test.ma")
                    .role(Role.CLIENT).status(Status.PENDING).isVerified(false).build();

            when(oauth2AccountRepository.findByProviderAndProviderUserId(OAuth2Account.Provider.GOOGLE, "g-777"))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail("existe@test.ma")).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(jwtService.generateAccessToken(any())).thenReturn("access-tok");
            when(refreshTokenService.create(any())).thenReturn(
                    RefreshToken.builder().token("refresh-tok").build());

            oauth2Service.googleLogin("fake-id-token");

            verify(userRepository, never()).save(argThat(u -> u.getId() == null));
            verify(oauth2AccountRepository).save(argThat(acc -> acc.getUser().equals(existingUser)));
            assertThat(existingUser.isVerified()).isTrue();
            assertThat(existingUser.getStatus()).isEqualTo(Status.ACTIVE);
        }

        @Test
        @DisplayName("reuses the same OAuth2Account when the provider+providerUserId already exists")
        void reusesExistingOAuth2Account() throws Exception {
            when(oauth2Properties.getGoogle()).thenReturn(google);
            JsonNode payload = json("""
                {"aud":"real-google-client-id","email":"deja@test.ma","sub":"g-555"}
            """);
            when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(payload));

            User existingUser = User.builder().id(3L).email("deja@test.ma")
                    .role(Role.CLIENT).status(Status.ACTIVE).isVerified(true).build();
            OAuth2Account existingAccount = OAuth2Account.builder().user(existingUser).build();

            when(oauth2AccountRepository.findByProviderAndProviderUserId(OAuth2Account.Provider.GOOGLE, "g-555"))
                    .thenReturn(Optional.of(existingAccount));
            when(jwtService.generateAccessToken(any())).thenReturn("access-tok");
            when(refreshTokenService.create(any())).thenReturn(
                    RefreshToken.builder().token("refresh-tok").build());

            oauth2Service.googleLogin("fake-id-token");

            verify(userRepository, never()).findByEmail(anyString());
            verify(oauth2AccountRepository, never()).save(any());
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("facebookLogin()")
    class FacebookLogin {

        @Test
        @DisplayName("rejects an invalid Facebook token")
        void rejectsInvalidToken() throws Exception {
            when(oauth2Properties.getFacebook()).thenReturn(facebook);
            JsonNode debugResponse = json("""
                {"data":{"is_valid":false}}
            """);
            when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(debugResponse));

            assertThatThrownBy(() -> oauth2Service.facebookLogin("bad-token"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid Facebook token");
        }

        @Test
        @DisplayName("creates a new user from a valid Facebook token")
        void createsNewUserFromValidToken() throws Exception {
            when(oauth2Properties.getFacebook()).thenReturn(facebook);

            JsonNode debugResponse = json("""
                {"data":{"is_valid":true}}
            """);
            JsonNode profileResponse = json("""
                {"id":"fb-321","email":"fb@test.ma","first_name":"Youssef","last_name":"Alami"}
            """);
            when(responseSpec.bodyToMono(JsonNode.class))
                    .thenReturn(Mono.just(debugResponse))
                    .thenReturn(Mono.just(profileResponse));

            when(oauth2AccountRepository.findByProviderAndProviderUserId(OAuth2Account.Provider.FACEBOOK, "fb-321"))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail("fb@test.ma")).thenReturn(Optional.empty());
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(55L);
                return u;
            });
            when(jwtService.generateAccessToken(any())).thenReturn("access-tok");
            when(refreshTokenService.create(any())).thenReturn(
                    RefreshToken.builder().token("refresh-tok").build());

            AuthDtos.TokenResponse result = oauth2Service.facebookLogin("valid-fb-token");

            assertThat(result.getUserId()).isEqualTo(55L);
            verify(userRepository).save(argThat(u -> u.getFirstName().equals("Youssef")));
        }

        @Test
        @DisplayName("handles a Facebook profile with no email by leaving the user unverified")
        void handlesNoEmailFromFacebook() throws Exception {
            when(oauth2Properties.getFacebook()).thenReturn(facebook);

            JsonNode debugResponse = json("""
                {"data":{"is_valid":true}}
            """);
            JsonNode profileResponse = json("""
                {"id":"fb-999","first_name":"Anon"}
            """);
            when(responseSpec.bodyToMono(JsonNode.class))
                    .thenReturn(Mono.just(debugResponse))
                    .thenReturn(Mono.just(profileResponse));

            when(oauth2AccountRepository.findByProviderAndProviderUserId(OAuth2Account.Provider.FACEBOOK, "fb-999"))
                    .thenReturn(Optional.empty());
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(60L);
                return u;
            });
            when(jwtService.generateAccessToken(any())).thenReturn("access-tok");
            when(refreshTokenService.create(any())).thenReturn(
                    RefreshToken.builder().token("refresh-tok").build());

            oauth2Service.facebookLogin("valid-fb-token-no-email");

            verify(userRepository, never()).findByEmail(any());
            verify(userRepository).save(argThat(u -> !u.isVerified()));
        }
    }
}
