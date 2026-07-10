package com.bricopro.user.service;

import com.bricopro.banner.entity.Banner;
import com.bricopro.banner.repository.BannerRepository;
import com.bricopro.task.entity.Task;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.repository.UserRepository;
import com.bricopro.messaging.repository.MessageRepository;
import com.bricopro.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DigestService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final BannerRepository bannerRepository;

    public Map<String, Object> getDigest() {
        User client = getCurrentUser();

        long pendingTasks = taskRepository.countByClientIdAndStatus(client.getId(), Task.TaskStatus.PENDING);
        long activeWorkers = userRepository.countActiveWorkers();
        long unreadMessages = messageRepository.countUnreadByUser(client);
        long unreadNotifications = notificationRepository.countUnreadByUser(client);

        List<Banner> activeBanners = bannerRepository.findActiveBanners(LocalDateTime.now());
        boolean promoActive = !activeBanners.isEmpty();
        String promoText = promoActive ? activeBanners.get(0).getTitle() : null;

        Map<String, Object> digest = new HashMap<>();
        digest.put("pendingTasks", pendingTasks);
        digest.put("activeWorkers", activeWorkers);
        digest.put("unreadMessages", unreadMessages);
        digest.put("unreadNotifications", unreadNotifications);
        digest.put("promoActive", promoActive);
        digest.put("promoText", promoText);

        return digest;
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}