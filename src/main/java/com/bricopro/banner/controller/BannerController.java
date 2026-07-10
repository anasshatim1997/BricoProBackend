package com.bricopro.banner.controller;

import com.bricopro.banner.service.BannerService;
import com.bricopro.home.dto.BannerDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @GetMapping("/active")
    public List<BannerDto> getActiveBanners() {
        return bannerService.getActiveBanners();
    }
}