package com.bricopro.user.controller;

import com.bricopro.home.dto.RecentWorkerDto;
import com.bricopro.user.service.RecentWorkersService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients/me")
@RequiredArgsConstructor
public class RecentWorkersController {
    private final RecentWorkersService recentWorkersService;

    @GetMapping("/recent-workers")
    public List<RecentWorkerDto> getRecentWorkers() {
        return recentWorkersService.getRecentWorkersForClient();
    }
}