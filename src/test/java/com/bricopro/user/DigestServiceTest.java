package com.bricopro.user;

import com.bricopro.banner.entity.Banner;
import com.bricopro.banner.repository.BannerRepository;
import com.bricopro.messaging.repository.MessageRepository;
import com.bricopro.notification.repository.NotificationRepository;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.repository.UserRepository;
import com.bricopro.user.service.DigestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DigestService")
class DigestServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock UserRepository userRepository;
    @Mock MessageRepository messageRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock BannerRepository bannerRepository;

    @InjectMocks DigestService digestService;

    private User client;

    @BeforeEach
    void setup() {
        client = User.builder().id(3L).email("amina@test.ma").build();

        SecurityContext context = mock(SecurityContext.class);
        var auth = new UsernamePasswordAuthenticationToken("amina@test.ma", null);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        when(userRepository.findByEmail("amina@test.ma")).thenReturn(Optional.of(client));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("aggregates pending tasks, active workers, and unread counts")
    void aggregatesCounts() {
        when(taskRepository.countByClientIdAndStatus(3L, TaskStatus.PENDING)).thenReturn(2L);
        when(userRepository.countActiveWorkers()).thenReturn(150L);
        when(messageRepository.countUnreadByUser(client)).thenReturn(4L);
        when(notificationRepository.countUnreadByUser(client)).thenReturn(7L);
        when(bannerRepository.findActiveBanners(any())).thenReturn(List.of());

        Map<String, Object> digest = digestService.getDigest();

        assertThat(digest.get("pendingTasks")).isEqualTo(2L);
        assertThat(digest.get("activeWorkers")).isEqualTo(150L);
        assertThat(digest.get("unreadMessages")).isEqualTo(4L);
        assertThat(digest.get("unreadNotifications")).isEqualTo(7L);
    }

    @Test
    @DisplayName("REGRESSION: promoActive is false and promoText is null when no banner is currently active")
    void noActiveBannerMeansNoPromo() {
        when(taskRepository.countByClientIdAndStatus(anyLong(), any())).thenReturn(0L);
        when(userRepository.countActiveWorkers()).thenReturn(0L);
        when(messageRepository.countUnreadByUser(any())).thenReturn(0L);
        when(notificationRepository.countUnreadByUser(any())).thenReturn(0L);
        when(bannerRepository.findActiveBanners(any())).thenReturn(List.of());

        Map<String, Object> digest = digestService.getDigest();

        assertThat(digest.get("promoActive")).isEqualTo(false);
        assertThat(digest.get("promoText")).isNull();
    }

    @Test
    @DisplayName("REGRESSION: promoText reflects the real active banner's title, not a hardcoded string")
    void promoReflectsRealActiveBanner() {
        when(taskRepository.countByClientIdAndStatus(anyLong(), any())).thenReturn(0L);
        when(userRepository.countActiveWorkers()).thenReturn(0L);
        when(messageRepository.countUnreadByUser(any())).thenReturn(0L);
        when(notificationRepository.countUnreadByUser(any())).thenReturn(0L);

        Banner realBanner = Banner.builder().id(1L).title("Ramadan -15% sur toutes les prestations").build();
        when(bannerRepository.findActiveBanners(any())).thenReturn(List.of(realBanner));

        Map<String, Object> digest = digestService.getDigest();

        assertThat(digest.get("promoActive")).isEqualTo(true);
        assertThat(digest.get("promoText")).isEqualTo("Ramadan -15% sur toutes les prestations");
        assertThat(digest.get("promoText")).isNotEqualTo("-10% sur le nettoyage");
    }
}
