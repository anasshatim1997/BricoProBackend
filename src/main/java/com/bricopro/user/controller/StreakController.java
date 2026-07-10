package com.bricopro.user.controller;

import com.bricopro.user.service.StreakService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clients/me/streak")
@RequiredArgsConstructor
public class StreakController {

    private final StreakService streakService;

    @GetMapping
    public int getStreak() {
        return streakService.getCurrentStreak();
    }

    @PostMapping("/checkin")
    public int checkIn() {
        return streakService.checkIn();
    }
}