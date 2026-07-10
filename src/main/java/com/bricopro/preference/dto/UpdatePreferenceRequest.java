package com.bricopro.preference.dto;

import lombok.Data;

@Data
public class UpdatePreferenceRequest {
    private String language;
    private String theme;
    private Boolean pushEnabled;
    private Boolean emailEnabled;
    private Boolean smsEnabled;
    private Boolean marketingEnabled;
    private String defaultCity;
    private Double defaultLatitude;
    private Double defaultLongitude;
}
