package com.bricopro.activity.controller;

import com.bricopro.activity.ActivityService;
import com.bricopro.home.dto.ActivityDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients/me")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping("/activity")
    public List<ActivityDto> getActivity(@RequestParam(defaultValue = "20") int limit) {
        return activityService.getRecentActivity(limit);
    }
}