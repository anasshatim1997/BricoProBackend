package com.bricopro.user.controller;

import com.bricopro.user.service.DigestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/clients/me/digest")
@RequiredArgsConstructor
public class DigestController {

    private final DigestService digestService;

    @GetMapping
    public Map<String, Object> getDigest() {
        return digestService.getDigest();
    }
}