package com.bricopro.admin;

import com.bricopro.admin.service.AdminExportService;
import com.bricopro.analytics.WorkerPerformanceService;
import com.bricopro.analytics.WorkerPerformanceService.PerformanceReport;
import com.bricopro.payment.repository.PaymentRepository;
import com.bricopro.task.entity.Task;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Role;
import com.bricopro.user.entity.User.Status;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import com.bricopro.user.repository.UserRepository;
import com.bricopro.user.repository.WorkerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminExportService")
class AdminExportServiceTest {

    @Mock UserRepository userRepository;
    @Mock TaskRepository taskRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock WorkerProfileRepository workerProfileRepository;
    @Mock WorkerPerformanceService performanceService;

    @InjectMocks AdminExportService exportService;

    private User clientUser;
    private User workerUser;
    private Task completedTask;

    @BeforeEach
    void setup() {
        clientUser = User.builder()
                .id(1L).firstName("Zineb").lastName("Amrani")
                .email("zineb@test.ma").phone("+212600001111")
                .role(Role.CLIENT).status(Status.ACTIVE).isVerified(true)
                .createdAt(LocalDateTime.of(2025, 1, 15, 10, 0))
                .build();

        workerUser = User.builder()
                .id(2L).firstName("Saad").lastName("Tlemcani")
                .email("saad@test.ma").phone("+212600002222")
                .role(Role.WORKER).status(Status.ACTIVE).isVerified(true)
                .createdAt(LocalDateTime.of(2025, 2, 20, 9, 0))
                .build();

        completedTask = Task.builder()
                .id(5L).client(clientUser).worker(workerUser)
                .serviceType(ServiceType.CLEANING)
                .title("Spring cleaning").description("Full apartment clean")
                .address("Hay Riad, Rabat")
                .scheduledDate(LocalDate.of(2025, 3, 10))
                .scheduledStart(LocalTime.of(9, 0))
                .status(TaskStatus.COMPLETED)
                .agreedPrice(BigDecimal.valueOf(400))
                .isUrgent(false)
                .createdAt(LocalDateTime.of(2025, 3, 1, 10, 0))
                .build();
    }

    // ─── EXPORT USERS CSV ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("exportUsersCsv()")
    class ExportUsersCsv {

        @Test
        @DisplayName("returns non-empty byte array")
        void returnsBytes() {
            when(userRepository.findAll()).thenReturn(List.of(clientUser, workerUser));

            byte[] csv = exportService.exportUsersCsv();
            assertThat(csv).isNotEmpty();
        }

        @Test
        @DisplayName("CSV starts with UTF-8 BOM bytes")
        void hasUtf8Bom() {
            when(userRepository.findAll()).thenReturn(List.of(clientUser));

            byte[] csv = exportService.exportUsersCsv();
            assertThat(csv[0]).isEqualTo((byte) 0xEF);
            assertThat(csv[1]).isEqualTo((byte) 0xBB);
            assertThat(csv[2]).isEqualTo((byte) 0xBF);
        }

        @Test
        @DisplayName("CSV header row contains expected columns")
        void headerRowPresent() {
            when(userRepository.findAll()).thenReturn(List.of());

            byte[] csv = exportService.exportUsersCsv();
            String content = new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);
            assertThat(content).contains("ID");
            assertThat(content).contains("Email");
            assertThat(content).contains("Rôle");
            assertThat(content).contains("Statut");
        }

        @Test
        @DisplayName("CSV contains user data rows")
        void containsUserData() {
            when(userRepository.findAll()).thenReturn(List.of(clientUser, workerUser));

            byte[] csv = exportService.exportUsersCsv();
            String content = new String(csv, StandardCharsets.UTF_8);
            assertThat(content).contains("zineb@test.ma");
            assertThat(content).contains("saad@test.ma");
            assertThat(content).contains("CLIENT");
            assertThat(content).contains("WORKER");
        }

        @Test
        @DisplayName("CSV marks verified users correctly")
        void verifiedField() {
            when(userRepository.findAll()).thenReturn(List.of(clientUser));

            byte[] csv = exportService.exportUsersCsv();
            String content = new String(csv, StandardCharsets.UTF_8);
            assertThat(content).contains("Oui"); // isVerified = true
        }

        @Test
        @DisplayName("returns valid CSV with empty user list")
        void emptyUserList() {
            when(userRepository.findAll()).thenReturn(List.of());

            byte[] csv = exportService.exportUsersCsv();
            String content = new String(csv, StandardCharsets.UTF_8);
            // Only header, no data rows
            assertThat(content.lines().count()).isEqualTo(1L);
        }
    }

    // ─── EXPORT TASKS CSV ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("exportTasksCsv()")
    class ExportTasksCsv {

        @Test
        @DisplayName("returns byte array with task data")
        void returnsBytes() {
            when(taskRepository.findAll()).thenReturn(List.of(completedTask));

            byte[] csv = exportService.exportTasksCsv(null, null);
            String content = new String(csv, StandardCharsets.UTF_8);
            assertThat(content).contains("Spring cleaning");
        }

        @Test
        @DisplayName("header row contains expected columns")
        void headerRowPresent() {
            when(taskRepository.findAll()).thenReturn(List.of());

            byte[] csv = exportService.exportTasksCsv(null, null);
            String content = new String(csv, StandardCharsets.UTF_8);
            assertThat(content).contains("ID");
            assertThat(content).contains("Titre");
            assertThat(content).contains("Statut");
            assertThat(content).contains("Prix convenu");
        }

        @Test
        @DisplayName("from date filter excludes earlier tasks")
        void fromDateFilter() {
            when(taskRepository.findAll()).thenReturn(List.of(completedTask));

            // task scheduled 2025-03-10, filter from 2025-04-01 → should be excluded
            byte[] csv = exportService.exportTasksCsv(LocalDate.of(2025, 4, 1), null);
            String content = new String(csv, StandardCharsets.UTF_8);
            assertThat(content).doesNotContain("Spring cleaning");
        }

        @Test
        @DisplayName("to date filter excludes later tasks")
        void toDateFilter() {
            when(taskRepository.findAll()).thenReturn(List.of(completedTask));

            // task scheduled 2025-03-10, filter to 2025-02-28 → should be excluded
            byte[] csv = exportService.exportTasksCsv(null, LocalDate.of(2025, 2, 28));
            String content = new String(csv, StandardCharsets.UTF_8);
            assertThat(content).doesNotContain("Spring cleaning");
        }

        @Test
        @DisplayName("task within date range is included")
        void taskWithinRange() {
            when(taskRepository.findAll()).thenReturn(List.of(completedTask));

            byte[] csv = exportService.exportTasksCsv(
                    LocalDate.of(2025, 3, 1),
                    LocalDate.of(2025, 3, 31));
            String content = new String(csv, StandardCharsets.UTF_8);
            assertThat(content).contains("Spring cleaning");
        }

        @Test
        @DisplayName("unassigned worker shows 'Non assigné'")
        void unassignedWorker() {
            completedTask.setWorker(null);
            when(taskRepository.findAll()).thenReturn(List.of(completedTask));

            byte[] csv = exportService.exportTasksCsv(null, null);
            String content = new String(csv, StandardCharsets.UTF_8);
            assertThat(content).contains("Non assigné");
        }
    }

    // ─── EXPORT REVENUE CSV ───────────────────────────────────────────────────

    @Nested
    @DisplayName("exportRevenueCsv()")
    class ExportRevenueCsv {

        @Test
        @DisplayName("generates 12 monthly rows plus TOTAL row")
        void twelveRowsPlusTotal() {
            when(paymentRepository.sumPlatformFeeByMonthAndYear(anyInt(), eq(2025)))
                    .thenReturn(BigDecimal.valueOf(1000));

            byte[] csv = exportService.exportRevenueCsv(2025);
            // Skip BOM (3 bytes) before decoding
            String content = new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);
            long dataRows = content.lines()
                    .filter(l -> !l.isBlank())
                    .skip(1)
                    .count();
            assertThat(dataRows).isEqualTo(13);
        }
        @Test
        @DisplayName("TOTAL row shows sum of all months")
        void totalRowCorrect() {
            when(paymentRepository.sumPlatformFeeByMonthAndYear(anyInt(), eq(2025)))
                    .thenReturn(BigDecimal.valueOf(500));

            byte[] csv = exportService.exportRevenueCsv(2025);
            String content = new String(csv, StandardCharsets.UTF_8);

            // 12 months × 500 = 6000
            assertThat(content).contains("6000");
            assertThat(content).contains("TOTAL");
        }

        @Test
        @DisplayName("handles null monthly revenue as zero")
        void nullMonthlyRevenue() {
            when(paymentRepository.sumPlatformFeeByMonthAndYear(anyInt(), eq(2025)))
                    .thenReturn(null);

            byte[] csv = exportService.exportRevenueCsv(2025);
            String content = new String(csv, StandardCharsets.UTF_8);
            assertThat(content).isNotEmpty();
            assertThat(content).contains("TOTAL,2025,0");
        }

        @Test
        @DisplayName("CSV has UTF-8 BOM")
        void hasUtf8Bom() {
            when(paymentRepository.sumPlatformFeeByMonthAndYear(anyInt(), anyInt()))
                    .thenReturn(BigDecimal.ZERO);

            byte[] csv = exportService.exportRevenueCsv(2025);
            assertThat(csv[0]).isEqualTo((byte) 0xEF);
            assertThat(csv[1]).isEqualTo((byte) 0xBB);
            assertThat(csv[2]).isEqualTo((byte) 0xBF);
        }
    }

    // ─── EXPORT REVENUE PDF ───────────────────────────────────────────────────

    @Nested
    @DisplayName("exportRevenuePdf()")
    class ExportRevenuePdf {

        @Test
        @DisplayName("returns non-empty PDF byte array")
        void returnsPdfBytes() {
            when(paymentRepository.sumPlatformFeeByMonthAndYear(anyInt(), anyInt()))
                    .thenReturn(BigDecimal.valueOf(2000));

            byte[] pdf = exportService.exportRevenuePdf(2025);
            assertThat(pdf).isNotEmpty();
        }

        @Test
        @DisplayName("PDF starts with %PDF magic bytes")
        void pdfMagicBytes() {
            when(paymentRepository.sumPlatformFeeByMonthAndYear(anyInt(), anyInt()))
                    .thenReturn(BigDecimal.ZERO);

            byte[] pdf = exportService.exportRevenuePdf(2025);
            String header = new String(pdf, 0, 4, StandardCharsets.US_ASCII);
            assertThat(header).isEqualTo("%PDF");
        }

        @Test
        @DisplayName("handles null revenue without throwing")
        void nullRevenue() {
            when(paymentRepository.sumPlatformFeeByMonthAndYear(anyInt(), anyInt()))
                    .thenReturn(null);

            assertThatNoException().isThrownBy(() -> exportService.exportRevenuePdf(2025));
        }
    }
}
