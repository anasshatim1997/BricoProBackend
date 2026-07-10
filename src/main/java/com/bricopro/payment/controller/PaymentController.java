package com.bricopro.payment.controller;

import com.bricopro.payment.dto.PaymentDtos.*;
import com.bricopro.payment.service.PaymentService;
import com.bricopro.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(
        summary = "Initiate payment for a completed task",
        description = "Initiate. Validates the incoming request body, persists the new resource, and returns the created entity with its server-generated ID."
    )
    public ResponseEntity<PaymentResponse> initiate(@AuthenticationPrincipal User user,
                                                     @Valid @RequestBody InitiatePaymentRequest req) {
        return ResponseEntity.ok(paymentService.initiate(user.getId(), req));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/mine/client")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(
        summary = "Get client payment history",
        description = "Client Payments. Returns a paginated list of matching resources. Use the query parameters to filter and control sort order."
    )
    public ResponseEntity<Page<PaymentResponse>> clientPayments(@AuthenticationPrincipal User user,
                                                                 Pageable pageable) {
        return ResponseEntity.ok(paymentService.getClientPayments(user.getId(), pageable));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/mine/worker")
    @PreAuthorize("hasRole('WORKER')")
    @Operation(
        summary = "Get worker payment history",
        description = "Worker Payments. Returns a paginated list of matching resources. Use the query parameters to filter and control sort order."
    )
    public ResponseEntity<Page<PaymentResponse>> workerPayments(@AuthenticationPrincipal User user,
                                                                 Pageable pageable) {
        return ResponseEntity.ok(paymentService.getWorkerPayments(user.getId(), pageable));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/revenue")
    @PreAuthorize("hasRole('WORKER')")
    @Operation(
        summary = "Get worker monthly revenue",
        description = "Worker Revenue. Returns a paginated list of matching resources. Use the query parameters to filter and control sort order."
    )
    public ResponseEntity<WorkerRevenueResponse> workerRevenue(@AuthenticationPrincipal User user,
                                                                @Parameter(name = "month", description = "Query filter or pagination parameter", required = true, in = ParameterIn.QUERY) @RequestParam int month,
                                                                @Parameter(name = "param", description = "Query filter or pagination parameter", required = true, in = ParameterIn.QUERY) @RequestParam int year) {
        return ResponseEntity.ok(paymentService.getWorkerRevenue(user.getId(), month, year));
    }

    @Operation(
        summary = "Confirm you paid this task in cash",
        description = "For CASH payments only. The client confirms they paid the worker in person, outside the app. " +
                "The payment is only marked COMPLETED once both the client and the worker have confirmed."
    )
    @PostMapping("/{paymentId}/confirm/client")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<PaymentResponse> confirmByClient(@AuthenticationPrincipal User user,
                                                            @PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.confirmCashByClient(paymentId, user.getId()));
    }

    @Operation(
        summary = "Confirm you received this task's cash payment",
        description = "For CASH payments only. The worker confirms they received the cash payment in person. " +
                "The payment is only marked COMPLETED once both the client and the worker have confirmed."
    )
    @PostMapping("/{paymentId}/confirm/worker")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<PaymentResponse> confirmByWorker(@AuthenticationPrincipal User user,
                                                            @PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.confirmCashByWorker(paymentId, user.getId()));
    }

    @Operation(
        summary = "Dispute a cash payment",
        description = "For CASH payments only. Either the client or the worker can raise a dispute if they disagree " +
                "about whether the cash payment actually happened. Flags the payment for admin review."
    )
    @PostMapping("/{paymentId}/dispute")
    @PreAuthorize("hasAnyRole('CLIENT', 'WORKER')")
    public ResponseEntity<PaymentResponse> disputeCashPayment(@AuthenticationPrincipal User user,
                                                               @PathVariable Long paymentId,
                                                               @RequestBody DisputePaymentRequest req) {
        return ResponseEntity.ok(paymentService.disputeCashPayment(paymentId, user.getId(), req.getReason()));
    }

    @Operation(
        summary = "CMI payment gateway webhook",
        description = "Receives the asynchronous payment confirmation POST from CMI's hosted payment page. " +
                "Not authenticated with a JWT — authenticity is verified via HMAC signature inside the service."
    )
    @PostMapping("/webhook/cmi")
    public ResponseEntity<Void> cmiWebhook(@RequestParam Map<String, String> params) {
        paymentService.handleGatewayCallback("CMI", params);
        return ResponseEntity.ok().build();
    }
}
