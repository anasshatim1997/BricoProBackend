package com.bricopro.preference.service;

import com.bricopro.preference.dto.UpdatePreferenceRequest;
import com.bricopro.preference.entity.UserPreference;
import com.bricopro.preference.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {

    private final UserPreferenceRepository prefRepository;

    public UserPreference getOrCreate(Long userId) {
        return prefRepository.findById(userId).orElseGet(() -> {
            UserPreference p = UserPreference.builder().userId(userId).build();
            return prefRepository.save(p);
        });
    }

    @Transactional
    public UserPreference update(Long userId, UpdatePreferenceRequest req) {
        UserPreference pref = getOrCreate(userId);
        if (req.getLanguage()         != null) pref.setLanguage(req.getLanguage());
        if (req.getTheme()            != null) pref.setTheme(req.getTheme());
        if (req.getPushEnabled()      != null) pref.setPushEnabled(req.getPushEnabled());
        if (req.getEmailEnabled()     != null) pref.setEmailEnabled(req.getEmailEnabled());
        if (req.getSmsEnabled()       != null) pref.setSmsEnabled(req.getSmsEnabled());
        if (req.getMarketingEnabled() != null) pref.setMarketingEnabled(req.getMarketingEnabled());
        if (req.getDefaultCity()      != null) pref.setDefaultCity(req.getDefaultCity());
        if (req.getDefaultLatitude()  != null) pref.setDefaultLatitude(req.getDefaultLatitude());
        if (req.getDefaultLongitude() != null) pref.setDefaultLongitude(req.getDefaultLongitude());
        return prefRepository.save(pref);
    }
}
