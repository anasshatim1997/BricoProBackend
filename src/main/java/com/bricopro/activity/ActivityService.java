package com.bricopro.activity;

import com.bricopro.home.dto.ActivityDto;
import com.bricopro.notification.entity.Notification;
import com.bricopro.notification.repository.NotificationRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public List<ActivityDto> getRecentActivity(int limit) {
        User user = getCurrentUser();
        PageRequest page = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user, page).getContent();

        return notifications.stream()
                .map(n -> ActivityDto.builder()
                        .id(n.getId().toString())
                        .type(n.getType().name())
                        .title(n.getTitle())
                        .description(n.getBody())
                        .timestamp(n.getCreatedAt())
                        .read(n.isRead())
                        .targetId(n.getReferenceId() != null ? n.getReferenceId().toString() : null)
                        .build())
                .collect(Collectors.toList());
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}