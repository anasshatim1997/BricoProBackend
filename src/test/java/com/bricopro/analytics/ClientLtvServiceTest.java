package com.bricopro.analytics;

import com.bricopro.analytics.service.ClientLtvService;
import com.bricopro.payment.repository.PaymentRepository;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.TaskRepository;
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
@DisplayName("ClientLtvService")
class ClientLtvServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock PaymentRepository paymentRepository;

    @InjectMocks ClientLtvService ltvService;

    @Test
    @DisplayName("classifies a big spender as VIP")
    void classifiesVip() {
        when(taskRepository.countByClientId(5L)).thenReturn(20L);
        when(taskRepository.findByClientIdAndStatus(eq(5L), eq(TaskStatus.COMPLETED), any(Pageable.class)))
                .thenReturn((Page) new PageImpl<>(java.util.Collections.nCopies(15, new Object())));
        when(paymentRepository.sumAllClientSpent(5L)).thenReturn(new BigDecimal("6000"));

        var report = ltvService.getClientLtv(5L);

        assertThat(report.getSegment()).isEqualTo("VIP");
        assertThat(report.getAvgSpentPerTask()).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("classifies a moderate spender as REGULAR")
    void classifiesRegular() {
        when(taskRepository.countByClientId(5L)).thenReturn(5L);
        when(taskRepository.findByClientIdAndStatus(eq(5L), eq(TaskStatus.COMPLETED), any(Pageable.class)))
                .thenReturn((Page) new PageImpl<>(java.util.Collections.nCopies(4, new Object())));
        when(paymentRepository.sumAllClientSpent(5L)).thenReturn(new BigDecimal("1500"));

        var report = ltvService.getClientLtv(5L);

        assertThat(report.getSegment()).isEqualTo("REGULAR");
    }

    @Test
    @DisplayName("classifies a first-timer as NEW")
    void classifiesNew() {
        when(taskRepository.countByClientId(5L)).thenReturn(1L);
        when(taskRepository.findByClientIdAndStatus(eq(5L), eq(TaskStatus.COMPLETED), any(Pageable.class)))
                .thenReturn((Page) new PageImpl<>(java.util.Collections.emptyList()));
        when(paymentRepository.sumAllClientSpent(5L)).thenReturn(null);

        var report = ltvService.getClientLtv(5L);

        assertThat(report.getSegment()).isEqualTo("NEW");
        assertThat(report.getTotalSpent()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.getAvgSpentPerTask()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
