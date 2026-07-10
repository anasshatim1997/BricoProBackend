package com.bricopro.analytics;

import com.bricopro.analytics.service.PlatformRevenueService;
import com.bricopro.payment.repository.PaymentRepository;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User.Role;
import com.bricopro.user.entity.User.Status;
import com.bricopro.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlatformRevenueService")
class PlatformRevenueServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock UserRepository userRepository;
    @Mock TaskRepository taskRepository;

    @InjectMocks PlatformRevenueService revenueService;

    @Test
    @DisplayName("sums 12 months of revenue and includes active-user/task counts")
    void sumsTwelveMonthsAndCounts() {
        when(paymentRepository.sumPlatformFeeByMonthAndYear(anyInt(), eq(2026)))
                .thenReturn(new BigDecimal("500"));
        when(userRepository.search(eq(Role.WORKER), eq(Status.ACTIVE), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.Collections.emptyList(), Pageable.ofSize(1), 150));
        when(userRepository.search(eq(Role.CLIENT), eq(Status.ACTIVE), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.Collections.emptyList(), Pageable.ofSize(1), 300));
        when(taskRepository.count()).thenReturn(1200L);

        var report = revenueService.getReport(2026);

        assertThat(report.getTotalRevenue()).isEqualByComparingTo("6000");
        assertThat(report.getRevenueByMonth()).hasSize(12);
        assertThat(report.getActiveWorkers()).isEqualTo(150L);
        assertThat(report.getActiveClients()).isEqualTo(300L);
        assertThat(report.getTotalTasks()).isEqualTo(1200L);
    }

    @Test
    @DisplayName("treats a null monthly fee as zero rather than throwing")
    void treatsNullMonthlyFeeAsZero() {
        when(paymentRepository.sumPlatformFeeByMonthAndYear(anyInt(), eq(2026))).thenReturn(null);
        when(userRepository.search(any(), any(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.Collections.emptyList()));
        when(taskRepository.count()).thenReturn(0L);

        var report = revenueService.getReport(2026);

        assertThat(report.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
