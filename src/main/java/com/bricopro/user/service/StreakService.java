package com.bricopro.user.service;

import com.bricopro.user.entity.User;
import com.bricopro.user.entity.UserStreak;
import com.bricopro.user.repository.UserRepository;
import com.bricopro.user.repository.UserStreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class StreakService {

    private final UserStreakRepository streakRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public int getCurrentStreak() {
        User user = getCurrentUser();
        return streakRepository.findByUser(user)
                .map(UserStreak::getCurrentStreak)
                .orElse(0);
    }

    @Transactional
    public int checkIn() {
        User user = getCurrentUser();
        UserStreak streak = streakRepository.findByUser(user)
                .orElse(UserStreak.builder()
                        .user(user)
                        .currentStreak(0)
                        .maxStreak(0)
                        .lastActiveDate(LocalDate.now().minusDays(1))
                        .build());

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        if (streak.getLastActiveDate().equals(today)) {
            // Already checked in today, just return current streak
            return streak.getCurrentStreak();
        }

        if (streak.getLastActiveDate().equals(yesterday)) {
            // Consecutive day, increase streak
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
        } else {
            // Reset streak
            streak.setCurrentStreak(1);
        }

        if (streak.getCurrentStreak() > streak.getMaxStreak()) {
            streak.setMaxStreak(streak.getCurrentStreak());
        }

        streak.setLastActiveDate(today);
        streakRepository.save(streak);
        return streak.getCurrentStreak();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}