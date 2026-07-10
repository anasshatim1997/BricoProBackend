package com.bricopro.service.controller;

import com.bricopro.home.dto.ServiceDto;
import com.bricopro.service.ServiceCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceCategoryService serviceCategoryService;

    @GetMapping
    public List<ServiceDto> getServices() {
        return serviceCategoryService.getAllActive();
    }
}