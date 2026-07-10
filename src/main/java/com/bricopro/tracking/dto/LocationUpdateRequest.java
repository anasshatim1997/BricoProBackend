package com.bricopro.tracking.dto;

import lombok.Data;

@Data
public class LocationUpdateRequest {
    private Double latitude;
    private Double longitude;
    private Double speedKmh;
    private Double headingDegrees;
    private Long   clientId;
    private Long   taskId;
}
