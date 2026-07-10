package com.bricopro.booking.dto;

import com.bricopro.user.entity.WorkerProfile.ServiceType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CreateGroupBookingRequest {
    private ServiceType serviceType;
    private String title;
    private String description;
    private String address;
    private LocalDate scheduledDate;
    private LocalTime scheduledStart;
    private int workersNeeded;
    private BigDecimal budgetPerWorker;
}
