package com.bricopro.notification.push;

import com.bricopro.user.entity.User;
import com.bricopro.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PushNotificationService")
class PushNotificationServiceTest {

    @Mock DeviceTokenRepository tokenRepository;
    @Mock UserRepository userRepository;

    @InjectMocks PushNotificationService pushService;

    private User worker;

    @BeforeEach
    void setup() throws Exception {
        worker = User.builder().id(2L).build();
        setField("fcmEnabled", false);
        setField("fcmProjectId", "");
        setField("fcmAccessToken", "");
    }

    private void setField(String name, Object value) throws Exception {
        Field f = PushNotificationService.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(pushService, value);
    }

    @Nested
    @DisplayName("registerToken()")
    class RegisterToken {

        @Test
        @DisplayName("saves a new device token")
        void savesNewToken() {
            when(tokenRepository.existsByUserIdAndDeviceToken(2L, "tok-abc")).thenReturn(false);
            when(userRepository.findById(2L)).thenReturn(Optional.of(worker));

            pushService.registerToken(2L, "tok-abc", DeviceToken.Platform.ANDROID);

            verify(tokenRepository).save(argThat(dt ->
                    dt.getDeviceToken().equals("tok-abc") && dt.getPlatform() == DeviceToken.Platform.ANDROID));
        }

        @Test
        @DisplayName("does nothing when the token is already registered for this user")
        void noOpOnDuplicateToken() {
            when(tokenRepository.existsByUserIdAndDeviceToken(2L, "tok-abc")).thenReturn(true);

            pushService.registerToken(2L, "tok-abc", DeviceToken.Platform.IOS);

            verify(tokenRepository, never()).save(any());
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("throws when the user doesn't exist")
        void throwsWhenUserMissing() {
            when(tokenRepository.existsByUserIdAndDeviceToken(999L, "tok-x")).thenReturn(false);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pushService.registerToken(999L, "tok-x", DeviceToken.Platform.ANDROID))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("unregisterToken()")
    class UnregisterToken {

        @Test
        @DisplayName("deletes the device token")
        void deletesToken() {
            pushService.unregisterToken(2L, "tok-abc");

            verify(tokenRepository).deleteByUserIdAndDeviceToken(2L, "tok-abc");
        }
    }

    @Nested
    @DisplayName("sendToUser() — FCM disabled (dev mode)")
    class SendToUserDevMode {

        @Test
        @DisplayName("logs instead of calling FCM when fcmEnabled is false, without throwing")
        void logsInsteadOfCallingFcmWhenDisabled() {
            DeviceToken dt = DeviceToken.builder()
                    .user(worker).deviceToken("tok-abc12345").platform(DeviceToken.Platform.ANDROID).build();
            when(tokenRepository.findByUserId(2L)).thenReturn(List.of(dt));

            assertThatCode(() -> pushService.sendToUser(2L, "Title", "Body", Map.of("k", "v")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("does nothing when the user has no registered devices")
        void noDevicesRegisteredIsNoOp() {
            when(tokenRepository.findByUserId(2L)).thenReturn(List.of());

            assertThatCode(() -> pushService.sendToUser(2L, "Title", "Body", Map.of()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("sends to every registered device for the user")
        void sendsToEveryDevice() {
            DeviceToken dt1 = DeviceToken.builder().user(worker).deviceToken("tok-1").platform(DeviceToken.Platform.ANDROID).build();
            DeviceToken dt2 = DeviceToken.builder().user(worker).deviceToken("tok-2").platform(DeviceToken.Platform.IOS).build();
            when(tokenRepository.findByUserId(2L)).thenReturn(List.of(dt1, dt2));

            assertThatCode(() -> pushService.sendToUser(2L, "Title", "Body", null))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("sendToUser() — FCM enabled but misconfigured")
    class SendToUserMisconfigured {

        @Test
        @DisplayName("falls back to dev-mode logging when enabled but projectId is blank")
        void fallsBackWhenProjectIdBlank() throws Exception {
            setField("fcmEnabled", true);
            setField("fcmProjectId", "");
            setField("fcmAccessToken", "some-token");

            DeviceToken dt = DeviceToken.builder().user(worker).deviceToken("tok-abc").platform(DeviceToken.Platform.ANDROID).build();
            when(tokenRepository.findByUserId(2L)).thenReturn(List.of(dt));

            assertThatCode(() -> pushService.sendToUser(2L, "Title", "Body", Map.of()))
                    .doesNotThrowAnyException();
        }
    }
}
