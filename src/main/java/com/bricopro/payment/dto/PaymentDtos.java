package com.bricopro.payment.dto;

import com.bricopro.payment.entity.Payment.PaymentMethod;
import com.bricopro.payment.entity.Payment.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

public class PaymentDtos {

    @Data
    @Schema(description = "Request payload for: Initiate Payment.")
    public static class InitiatePaymentRequest {
        @NotNull
        @Schema(description = "ID of the related task", example = "1")
        private Long taskId;
        @NotNull
        @Schema(description = "Payment method: CMI, CIH_PAY, BANK_TRANSFER, or CASH", example = "value")
        private PaymentMethod method;
    }

    @Data
    @Schema(description = "Request payload for: Dispute Payment.")
    public static class DisputePaymentRequest {
        @jakarta.validation.constraints.NotBlank
        @Schema(description = "Reason for disputing this cash payment", example = "Worker says they never received the cash")
        private String reason;
    }

    @Data
    @Schema(description = "Response body returned by: Payment.")
    public static class PaymentResponse {
        @Schema(description = "Unique identifier", example = "1")
        private Long id;
        @Schema(description = "ID of the related task", example = "1")
        private Long taskId;
        @Schema(description = "Gross Amount", example = "150.00")
        private BigDecimal grossAmount;
        @Schema(description = "Platform Fee", example = "150.00")
        private BigDecimal platformFee;
        @Schema(description = "Processing Fee", example = "150.00")
        private BigDecimal processingFee;
        @Schema(description = "Net Amount", example = "150.00")
        private BigDecimal netAmount;
        @Schema(description = "Currency", example = "example")
        private String currency;
        @Schema(description = "Payment method: CMI, CIH_PAY, BANK_TRANSFER, or CASH", example = "value")
        private PaymentMethod method;
        @Schema(description = "Current status of the entity", example = "value")
        private PaymentStatus status;
        @Schema(description = "Gateway Reference", example = "example")
        private String gatewayReference;
        @Schema(description = "URL to redirect the client to for gateways that require it (e.g. CMI 3D-Secure page). Null for synchronous methods like cash.", example = "https://testpayment.cmi.co.ma/fim/est3Dgate?...")
        private String redirectUrl;
        @Schema(description = "For CASH payments: whether the client has confirmed paying in person", example = "false")
        private boolean clientConfirmedPayment;
        @Schema(description = "For CASH payments: whether the worker has confirmed receiving the cash", example = "false")
        private boolean workerConfirmedReceipt;
        @Schema(description = "Paid At", example = "2025-06-15")
        private LocalDateTime paidAt;
        @Schema(description = "ISO-8601 timestamp when the record was created", example = "2025-06-15")
        private LocalDateTime createdAt;
    }

    @Data
    @Schema(description = "Response body returned by: Worker Revenue.")
    public static class WorkerRevenueResponse {
        @Schema(description = "Total Revenue", example = "150.00")
        private BigDecimal totalRevenue;
        @Schema(description = "Month number — 1 = January, 12 = December", example = "0")
        private int month;
        @Schema(description = "Four-digit calendar year (e.g. 2025)", example = "0")
        private int year;
    }
}
