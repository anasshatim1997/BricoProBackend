package com.bricopro.activity;

import com.bricopro.notification.entity.Notification;
import com.bricopro.notification.entity.Notification.NotificationType;
import com.bricopro.notification.repository.NotificationRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityService")
class ActivityServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock UserRepository userRepository;

    @InjectMocks ActivityService activityService;

    private User client;

    @BeforeEach
    void setup() {
        client = User.builder().id(3L).email("amina@test.ma").build();

        SecurityContext context = mock(SecurityContext.class);
        var auth = new UsernamePasswordAuthenticationToken("amina@test.ma", null);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("REGRESSION: resolves the real current user instead of the old null-returning stub")
    void resolvesRealCurrentUserNotNull() {
        when(userRepository.findByEmail("amina@test.ma")).thenReturn(Optional.of(client));
        when(notificationRepository.findByUserOrderByCreatedAtDesc(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        activityService.getRecentActivity(10);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(notificationRepository).findByUserOrderByCreatedAtDesc(userCaptor.capture(), any());
        assertThat(userCaptor.getValue()).isNotNull();
        assertThat(userCaptor.getValue().getId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("maps notifications to activity DTOs correctly")
    void mapsNotificationsToDto() {
        when(userRepository.findByEmail("amina@test.ma")).thenReturn(Optional.of(client));

        Notification notif = Notification.builder()
                .id(100L).user(client).type(NotificationType.PAYMENT_RECEIVED)
                .title("Payment received").body("250 MAD credited")
                .createdAt(LocalDateTime.now()).isRead(false).referenceId(55L)
                .build();

        when(notificationRepository.findByUserOrderByCreatedAtDesc(eq(client), any()))
                .thenReturn(new PageImpl<>(List.of(notif)));

        var result = activityService.getRecentActivity(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("100");
        assertThat(result.get(0).getType()).isEqualTo("PAYMENT_RECEIVED");
        assertThat(result.get(0).getTitle()).isEqualTo("Payment received");
        assertThat(result.get(0).isRead()).isFalse();
        assertThat(result.get(0).getTargetId()).isEqualTo("55");
    }

    @Test
    @DisplayName("handles a notification with no reference id")
    void handlesNullReferenceId() {
        when(userRepository.findByEmail("amina@test.ma")).thenReturn(Optional.of(client));

        Notification notif = Notification.builder()
                .id(101L).user(client).type(NotificationType.SYSTEM)
                .title("Welcome").body("Welcome to BricoPro")
                .createdAt(LocalDateTime.now()).isRead(true).referenceId(null)
                .build();

        when(notificationRepository.findByUserOrderByCreatedAtDesc(eq(client), any()))
                .thenReturn(new PageImpl<>(List.of(notif)));

        var result = activityService.getRecentActivity(10);

        assertThat(result.get(0).getTargetId()).isNull();
    }

    @Test
    @DisplayName("respects the requested page size limit")
    void respectsLimit() {
        when(userRepository.findByEmail("amina@test.ma")).thenReturn(Optional.of(client));
        when(notificationRepository.findByUserOrderByCreatedAtDesc(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        activityService.getRecentActivity(5);

        ArgumentCaptor<PageRequest> pageCaptor = ArgumentCaptor.forClass(PageRequest.class);
        verify(notificationRepository).findByUserOrderByCreatedAtDesc(any(), pageCaptor.capture());
        assertThat(pageCaptor.getValue().getPageSize()).isEqualTo(5);
    }
}
