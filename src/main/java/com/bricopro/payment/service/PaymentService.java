package com.bricopro.payment.service;

import com.bricopro.notification.service.CommunicationService;
import com.bricopro.notification.service.NotificationService;
import com.bricopro.payment.dto.PaymentDtos.*;
import com.bricopro.payment.entity.Payment;
import com.bricopro.payment.entity.Payment.PaymentStatus;
import com.bricopro.payment.entity.PlatformRevenue;
import com.bricopro.payment.gateway.PaymentGateway;
import com.bricopro.payment.gateway.PaymentGateway.GatewayResult;
import com.bricopro.payment.mapper.PaymentMapper;
import com.bricopro.payment.repository.PaymentRepository;
import com.bricopro.payment.repository.PlatformRevenueRepository;
import com.bricopro.task.entity.Task;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.TaskRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Payment Service", description = "Business logic for Payment Service")
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.12");
    private static final BigDecimal PROCESSING_RATE = new BigDecimal("0.015");

    private final PaymentRepository         paymentRepository;
    private final PlatformRevenueRepository platformRevenueRepository;
    private final TaskRepository            taskRepository;
    private final NotificationService       notificationService;
    private final CommunicationService      communicationService;
    private final PaymentMapper             mapper;
    private final List<PaymentGateway>      gateways;

    private Map<String, PaymentGateway> gatewaysByName;

    @PostConstruct
    void indexGateways() {
        gatewaysByName = gateways.stream()
                .collect(Collectors.toMap(PaymentGateway::gatewayName, g -> g));
    }

    @Transactional
    public PaymentResponse initiate(Long clientId, InitiatePaymentRequest req) {
        Task task = taskRepository.findById(req.getTaskId())
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        if (!task.getClient().getId().equals(clientId))
            throw new SecurityException("Not your task");
        if (task.getStatus() != TaskStatus.COMPLETED)
            throw new IllegalStateException("Task must be completed before payment");
        if (task.getAgreedPrice() == null)
            throw new IllegalStateException("No agreed price on task");

        PaymentGateway gateway = gatewaysByName.get(req.getMethod().name());
        if (gateway == null) {
            throw new IllegalStateException(
                    "Payment method " + req.getMethod() + " is not yet supported. " +
                    "Available methods: " + gatewaysByName.keySet());
        }

        BigDecimal gross         = task.getAgreedPrice();
        BigDecimal platformFee   = gross.multiply(COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal processingFee = gross.multiply(PROCESSING_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal net           = gross.subtract(platformFee).subtract(processingFee);

        Payment payment = Payment.builder()
                .task(task)
                .client(task.getClient())
                .worker(task.getWorker())
                .grossAmount(gross)
                .platformFee(platformFee)
                .processingFee(processingFee)
                .netAmount(net)
                .method(req.getMethod())
                .status(PaymentStatus.PROCESSING)
                .build();

        payment = paymentRepository.save(payment);

        String clientRef = "BRICO-" + payment.getId();
        GatewayResult result = gateway.initiate(task.getId(), gross, clientRef);

        payment.setGatewayReference(result.reference());

        if (req.getMethod() == Payment.PaymentMethod.CASH) {
            payment.setStatus(PaymentStatus.AWAITING_CONFIRMATION);
        }

        payment = paymentRepository.save(payment);

        if (result.synchronous()) {
            completePayment(payment);
        } else if (req.getMethod() == Payment.PaymentMethod.CASH) {
            notificationService.notifyCashPaymentAwaitingConfirmation(payment.getWorker(), payment.getId());
            log.info("Payment {} awaiting mutual cash-on-delivery confirmation", payment.getId());
        } else {
            log.info("Payment {} awaiting async confirmation from {} gateway",
                    payment.getId(), gateway.gatewayName());
        }

        PaymentResponse response = mapper.toResponse(payment);
        response.setRedirectUrl(result.redirectUrl());
        return response;
    }

    @Transactional
    public PaymentResponse confirmCashByClient(Long paymentId, Long clientId) {
        Payment payment = requireCashAwaitingConfirmation(paymentId);
        if (!payment.getClient().getId().equals(clientId)) {
            throw new SecurityException("Not your payment");
        }
        payment.setClientConfirmedPayment(true);
        payment.setClientConfirmedAt(LocalDateTime.now());
        return finalizeIfBothConfirmed(payment);
    }

    @Transactional
    public PaymentResponse confirmCashByWorker(Long paymentId, Long workerId) {
        Payment payment = requireCashAwaitingConfirmation(paymentId);
        if (!payment.getWorker().getId().equals(workerId)) {
            throw new SecurityException("Not your payment");
        }
        payment.setWorkerConfirmedReceipt(true);
        payment.setWorkerConfirmedAt(LocalDateTime.now());
        return finalizeIfBothConfirmed(payment);
    }

    @Transactional
    public PaymentResponse disputeCashPayment(Long paymentId, Long actorId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        if (payment.getMethod() != Payment.PaymentMethod.CASH) {
            throw new IllegalStateException("This payment does not use cash-on-delivery confirmation");
        }
        if (payment.getStatus() != PaymentStatus.AWAITING_CONFIRMATION
                && payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Payment cannot be disputed in its current status: " + payment.getStatus());
        }
        boolean isParticipant = payment.getClient().getId().equals(actorId)
                || payment.getWorker().getId().equals(actorId);
        if (!isParticipant) {
            throw new SecurityException("Not a participant in this payment");
        }
        payment.setStatus(PaymentStatus.DISPUTED);
        payment = paymentRepository.save(payment);
        notificationService.notifyPaymentDisputed(payment.getId(), reason);
        return mapper.toResponse(payment);
    }

    private Payment requireCashAwaitingConfirmation(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        if (payment.getMethod() != Payment.PaymentMethod.CASH) {
            throw new IllegalStateException("This payment does not use cash-on-delivery confirmation");
        }
        if (payment.getStatus() != PaymentStatus.AWAITING_CONFIRMATION) {
            throw new IllegalStateException("Payment is not awaiting confirmation (current status: " + payment.getStatus() + ")");
        }
        return payment;
    }

    private PaymentResponse finalizeIfBothConfirmed(Payment payment) {
        payment = paymentRepository.save(payment);
        if (payment.isClientConfirmedPayment() && payment.isWorkerConfirmedReceipt()) {
            completePayment(payment);
            payment = paymentRepository.findById(payment.getId()).orElseThrow();
        }
        return mapper.toResponse(payment);
    }

    @Transactional
    public void handleGatewayCallback(String gatewayName, Map<String, String> params) {
        PaymentGateway gateway = gatewaysByName.get(gatewayName);
        if (gateway == null) {
            throw new IllegalArgumentException("Unknown gateway: " + gatewayName);
        }

        boolean confirmed = gateway.verifyCallback(params);
        if (!confirmed) {
            log.warn("{} callback verification failed for params={}", gatewayName, params.keySet());
            return;
        }

        String oid = params.get("oid");
        if (oid == null) {
            log.error("{} callback missing order id (oid)", gatewayName);
            return;
        }

        Payment payment = paymentRepository.findByGatewayReference(oid)
                .orElseThrow(() -> new IllegalStateException("No payment found for reference " + oid));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            log.info("Payment {} already completed, ignoring duplicate callback", payment.getId());
            return;
        }

        completePayment(payment);
    }

    @Transactional
    public void completePayment(Payment payment) {
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        if (payment.getGatewayReference() == null) {
            payment.setGatewayReference("BRICO-" + payment.getId());
        }
        paymentRepository.save(payment);

        if (!platformRevenueRepository.existsByPaymentId(payment.getId())) {
            platformRevenueRepository.save(PlatformRevenue.builder()
                    .payment(payment)
                    .amount(payment.getPlatformFee())
                    .month(payment.getPaidAt().getMonthValue())
                    .year(payment.getPaidAt().getYear())
                    .build());
        }

        notificationService.notifyPaymentReceived(
                payment.getWorker(),
                payment.getId(),
                payment.getNetAmount().toPlainString());

        if (payment.getWorker().getEmail() != null) {
            communicationService.sendPaymentConfirmationEmail(
                    payment.getWorker().getEmail(),
                    payment.getWorker().getFirstName(),
                    payment.getNetAmount().toPlainString());
        }

        if (payment.getWorker().getPhone() != null) {
            communicationService.sendWhatsApp(
                    payment.getWorker().getPhone(),
                    "BricoPro: Vous avez reçu un paiement de "
                            + payment.getNetAmount().toPlainString() + " MAD.");
        }
    }

    public Page<PaymentResponse> getClientPayments(Long clientId, Pageable pageable) {
        return paymentRepository.findByClientId(clientId, pageable).map(mapper::toResponse);
    }

    public Page<PaymentResponse> getWorkerPayments(Long workerId, Pageable pageable) {
        return paymentRepository.findByWorkerId(workerId, pageable).map(mapper::toResponse);
    }

    public WorkerRevenueResponse getWorkerRevenue(Long workerId, int month, int year) {
        BigDecimal total = paymentRepository.sumWorkerRevenueByMonth(workerId, month, year);
        WorkerRevenueResponse res = new WorkerRevenueResponse();
        res.setTotalRevenue(total != null ? total : BigDecimal.ZERO);
        res.setMonth(month);
        res.setYear(year);
        return res;
    }
}
