package com.bricopro.payment;

import com.bricopro.notification.service.CommunicationService;
import com.bricopro.notification.service.NotificationService;
import com.bricopro.payment.dto.PaymentDtos.*;
import com.bricopro.payment.entity.Payment;
import com.bricopro.payment.entity.Payment.PaymentMethod;
import com.bricopro.payment.entity.Payment.PaymentStatus;
import com.bricopro.payment.entity.PlatformRevenue;
import com.bricopro.payment.gateway.CashGateway;
import com.bricopro.payment.gateway.CmiGateway;
import com.bricopro.payment.gateway.PaymentGateway;
import com.bricopro.payment.mapper.PaymentMapper;
import com.bricopro.payment.repository.PaymentRepository;
import com.bricopro.payment.repository.PlatformRevenueRepository;
import com.bricopro.payment.service.PaymentService;
import com.bricopro.task.entity.Task;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Role;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService")
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock PlatformRevenueRepository platformRevenueRepository;
    @Mock TaskRepository taskRepository;
    @Mock NotificationService notificationService;
    @Mock CommunicationService communicationService;
    @Mock PaymentMapper mapper;

    PaymentService paymentService;

    private User client;
    private User worker;
    private Task completedTask;

    @BeforeEach
    void setup() throws Exception {
        client = User.builder().id(1L).firstName("Amina").lastName("Tazi")
                .email("amina@test.ma").role(Role.CLIENT).build();

        worker = User.builder().id(2L).firstName("Karim").lastName("Fassi")
                .email("karim@test.ma").phone("+212600000002").role(Role.WORKER).build();

        completedTask = Task.builder()
                .id(5L)
                .client(client)
                .worker(worker)
                .serviceType(ServiceType.CLEANING)
                .title("Apartment cleaning")
                .description("Full apartment clean")
                .address("Hay Riad, Rabat")
                .scheduledDate(LocalDate.now())
                .scheduledStart(LocalTime.of(9, 0))
                .status(TaskStatus.COMPLETED)
                .agreedPrice(BigDecimal.valueOf(500))
                .build();

        List<PaymentGateway> gateways = List.of(new CashGateway(), new CmiGateway());
        paymentService = new PaymentService(
                paymentRepository, platformRevenueRepository, taskRepository,
                notificationService, communicationService, mapper, gateways);

        Field indexMethod = PaymentService.class.getDeclaredMethod("indexGateways");
        indexMethod.setAccessible(true);
        indexMethod.invoke(paymentService);
    }

    // ─── INITIATE PAYMENT ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("initiate()")
    class Initiate {

        @Test
        @DisplayName("calculates platform fee (12%), processing fee (1.5%) and net correctly")
        void feesCalculatedCorrectly() {
            InitiatePaymentRequest req = new InitiatePaymentRequest();
            req.setTaskId(5L);
            req.setMethod(PaymentMethod.CASH);

            when(taskRepository.findById(5L)).thenReturn(Optional.of(completedTask));
            when(paymentRepository.save(any())).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p = Payment.builder()
                        .id(1L).task(completedTask).client(client).worker(worker)
                        .grossAmount(p.getGrossAmount())
                        .platformFee(p.getPlatformFee())
                        .processingFee(p.getProcessingFee())
                        .netAmount(p.getNetAmount())
                        .method(PaymentMethod.CASH)
                        .status(PaymentStatus.AWAITING_CONFIRMATION)
                        .build();
                return p;
            });
            PaymentResponse mockResponse = new PaymentResponse();
            when(mapper.toResponse(any())).thenReturn(mockResponse);

            paymentService.initiate(1L, req);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository, atLeast(1)).save(captor.capture());

            Payment captured = captor.getAllValues().get(0);
            assertThat(captured.getGrossAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
            assertThat(captured.getPlatformFee()).isEqualByComparingTo(BigDecimal.valueOf(60).setScale(2));
            assertThat(captured.getProcessingFee()).isEqualByComparingTo(new BigDecimal("7.50"));
            assertThat(captured.getNetAmount()).isEqualByComparingTo(new BigDecimal("432.50"));
        }

        @Test
        @DisplayName("sets CASH payments to AWAITING_CONFIRMATION instead of auto-completing")
        void cashAwaitsConfirmation() {
            InitiatePaymentRequest req = new InitiatePaymentRequest();
            req.setTaskId(5L);
            req.setMethod(PaymentMethod.CASH);

            when(taskRepository.findById(5L)).thenReturn(Optional.of(completedTask));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(any())).thenReturn(new PaymentResponse());

            paymentService.initiate(1L, req);

            verify(platformRevenueRepository, never()).save(any());
            verify(notificationService, never()).notifyPaymentReceived(any(), any(), anyString());
            verify(notificationService).notifyCashPaymentAwaitingConfirmation(eq(worker), any());

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository, atLeast(1)).save(captor.capture());
            assertThat(captor.getAllValues())
                    .anyMatch(p -> p.getStatus() == PaymentStatus.AWAITING_CONFIRMATION);
        }

        @Test
        @DisplayName("throws SecurityException when client doesn't own the task")
        void wrongClientThrows() {
            InitiatePaymentRequest req = new InitiatePaymentRequest();
            req.setTaskId(5L);
            req.setMethod(PaymentMethod.CASH);

            when(taskRepository.findById(5L)).thenReturn(Optional.of(completedTask));

            assertThatThrownBy(() -> paymentService.initiate(99L, req))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        @DisplayName("throws when task is not COMPLETED")
        void taskNotCompleted() {
            completedTask.setStatus(TaskStatus.CONFIRMED);

            InitiatePaymentRequest req = new InitiatePaymentRequest();
            req.setTaskId(5L);
            req.setMethod(PaymentMethod.CASH);

            when(taskRepository.findById(5L)).thenReturn(Optional.of(completedTask));

            assertThatThrownBy(() -> paymentService.initiate(1L, req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("completed");
        }

        @Test
        @DisplayName("throws when task has no agreed price")
        void noAgreedPrice() {
            completedTask.setAgreedPrice(null);

            InitiatePaymentRequest req = new InitiatePaymentRequest();
            req.setTaskId(5L);
            req.setMethod(PaymentMethod.CASH);

            when(taskRepository.findById(5L)).thenReturn(Optional.of(completedTask));

            assertThatThrownBy(() -> paymentService.initiate(1L, req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("price");
        }

        @Test
        @DisplayName("throws when the requested method has no registered gateway")
        void unsupportedMethodThrows() {
            InitiatePaymentRequest req = new InitiatePaymentRequest();
            req.setTaskId(5L);
            req.setMethod(PaymentMethod.BANK_TRANSFER);

            when(taskRepository.findById(5L)).thenReturn(Optional.of(completedTask));

            assertThatThrownBy(() -> paymentService.initiate(1L, req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not yet supported");
        }
    }

    // ─── CASH CONFIRMATION FLOW ────────────────────────────────────────────────

    @Nested
    @DisplayName("cash-on-delivery confirmation")
    class CashConfirmation {

        private Payment awaitingPayment() {
            return Payment.builder()
                    .id(10L).task(completedTask).client(client).worker(worker)
                    .grossAmount(BigDecimal.valueOf(500)).platformFee(BigDecimal.valueOf(60))
                    .processingFee(new BigDecimal("7.50")).netAmount(new BigDecimal("432.50"))
                    .method(PaymentMethod.CASH).status(PaymentStatus.AWAITING_CONFIRMATION)
                    .clientConfirmedPayment(false).workerConfirmedReceipt(false)
                    .build();
        }

        @Test
        @DisplayName("does not complete after only the client confirms")
        void onlyClientConfirmedStaysAwaiting() {
            Payment payment = awaitingPayment();
            when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(any())).thenReturn(new PaymentResponse());

            paymentService.confirmCashByClient(10L, 1L);

            assertThat(payment.isClientConfirmedPayment()).isTrue();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AWAITING_CONFIRMATION);
            verify(platformRevenueRepository, never()).save(any());
        }

        @Test
        @DisplayName("completes once both client and worker have confirmed")
        void bothConfirmedCompletes() {
            Payment payment = awaitingPayment();
            payment.setClientConfirmedPayment(true);
            when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(platformRevenueRepository.existsByPaymentId(10L)).thenReturn(false);
            when(platformRevenueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(any())).thenReturn(new PaymentResponse());

            paymentService.confirmCashByWorker(10L, 2L);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            verify(platformRevenueRepository).save(any(PlatformRevenue.class));
            verify(notificationService).notifyPaymentReceived(eq(worker), any(), anyString());
        }

        @Test
        @DisplayName("rejects confirmation from someone who isn't the payment's client")
        void wrongClientCannotConfirm() {
            Payment payment = awaitingPayment();
            when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> paymentService.confirmCashByClient(10L, 999L))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        @DisplayName("rejects re-confirming a payment that's already completed")
        void cannotReconfirmCompleted() {
            Payment payment = awaitingPayment();
            payment.setStatus(PaymentStatus.COMPLETED);
            when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> paymentService.confirmCashByClient(10L, 1L))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("allows disputing while awaiting confirmation")
        void disputeWhileAwaiting() {
            Payment payment = awaitingPayment();
            when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(any())).thenReturn(new PaymentResponse());

            paymentService.disputeCashPayment(10L, 2L, "Never received the cash");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DISPUTED);
            verify(notificationService).notifyPaymentDisputed(eq(10L), anyString());
        }

        @Test
        @DisplayName("allows disputing after completion")
        void disputeAfterCompletion() {
            Payment payment = awaitingPayment();
            payment.setStatus(PaymentStatus.COMPLETED);
            when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(any())).thenReturn(new PaymentResponse());

            paymentService.disputeCashPayment(10L, 1L, "Worker never showed up despite marking paid");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DISPUTED);
        }

        @Test
        @DisplayName("rejects dispute from a non-participant")
        void nonParticipantCannotDispute() {
            Payment payment = awaitingPayment();
            when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> paymentService.disputeCashPayment(10L, 999L, "not involved"))
                    .isInstanceOf(SecurityException.class);
        }
    }

    // ─── GET WORKER REVENUE ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getWorkerRevenue()")
    class WorkerRevenue {

        @Test
        @DisplayName("returns revenue for given month and year")
        void returnsRevenue() {
            when(paymentRepository.sumWorkerRevenueByMonth(2L, 5, 2025))
                    .thenReturn(BigDecimal.valueOf(2500));

            WorkerRevenueResponse res = paymentService.getWorkerRevenue(2L, 5, 2025);

            assertThat(res.getTotalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(2500));
            assertThat(res.getMonth()).isEqualTo(5);
            assertThat(res.getYear()).isEqualTo(2025);
        }

        @Test
        @DisplayName("returns zero when no payments found")
        void returnsZeroWhenNull() {
            when(paymentRepository.sumWorkerRevenueByMonth(anyLong(), anyInt(), anyInt()))
                    .thenReturn(null);

            WorkerRevenueResponse res = paymentService.getWorkerRevenue(2L, 1, 2025);
            assertThat(res.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
