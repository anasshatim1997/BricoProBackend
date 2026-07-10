package com.bricopro.notification.push;

import com.bricopro.task.entity.Task;
import com.bricopro.user.entity.User;
import com.bricopro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
class PushNotificationService {

    private final DeviceTokenRepository tokenRepository;
    private final UserRepository userRepository;

    @Value("${app.fcm.project-id:}")
    private String fcmProjectId;

    @Value("${app.fcm.access-token:}")
    private String fcmAccessToken;

    @Value("${app.fcm.enabled:false}")
    private boolean fcmEnabled;

    @Transactional
    public void registerToken(Long userId, String token, DeviceToken.Platform platform) {
        if (tokenRepository.existsByUserIdAndDeviceToken(userId, token)) return;
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        tokenRepository.save(DeviceToken.builder()
                .user(user).deviceToken(token).platform(platform).build());
    }

    @Transactional
    public void unregisterToken(Long userId, String token) {
        tokenRepository.deleteByUserIdAndDeviceToken(userId, token);
    }

    public void sendToUser(Long userId, String title, String body, Map<String, String> data) {
        List<DeviceToken> tokens = tokenRepository.findByUserId(userId);
        tokens.forEach(dt -> sendToToken(dt.getDeviceToken(), title, body, data));
    }

    private void sendToToken(String token, String title, String body, Map<String, String> data) {
        if (!fcmEnabled || fcmProjectId.isBlank() || fcmAccessToken.isBlank()) {
            log.info("[DEV] FCM push (disabled) → token=...{} title='{}' body='{}'",
                    token.length() > 8 ? token.substring(token.length() - 8) : token, title, body);
            return;
        }

        try {
            RestTemplate rest = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(fcmAccessToken);

            String url = "https://fcm.googleapis.com/v1/projects/" + fcmProjectId + "/messages:send";

            Map<String, Object> notification = Map.of("title", title, "body", body);
            Map<String, Object> message = new HashMap<>();
            message.put("token", token);
            message.put("notification", notification);
            if (data != null && !data.isEmpty()) {
                message.put("data", data);
            }
            message.put("android", Map.of(
                    "priority", "high",
                    "notification", Map.of("sound", "default")
            ));
            message.put("apns", Map.of(
                    "headers", Map.of("apns-priority", "10"),
                    "payload", Map.of("aps", Map.of("sound", "default"))
            ));

            Map<String, Object> payload = Map.of("message", message);

            ResponseEntity<String> response = rest.postForEntity(
                    url, new HttpEntity<>(payload, headers), String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("FCM v1 push sent → token ...{}", token.substring(Math.max(0, token.length() - 8)));
            } else {
                log.error("FCM v1 push failed: status={} body={}", response.getStatusCode(), response.getBody());
            }

        } catch (Exception e) {
            log.error("FCM v1 push exception for token ...{}: {}",
                    token.length() > 8 ? token.substring(token.length() - 8) : token, e.getMessage());
        }
    }
}
