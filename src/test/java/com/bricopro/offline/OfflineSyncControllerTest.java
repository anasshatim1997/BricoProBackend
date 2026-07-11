package com.bricopro.offline;

import com.bricopro.config.SecurityConfig;
import com.bricopro.messaging.service.MessagingService;
import com.bricopro.security.filter.JwtAuthFilter;
import com.bricopro.security.oauth2.OAuth2SuccessHandler;
import com.bricopro.task.dto.TaskDtos.TaskResponse;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.service.TaskService;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OfflineSyncController.class)
@Import(SecurityConfig.class)
class OfflineSyncControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean TaskService taskService;
    @MockitoBean MessagingService messagingService;
    @MockitoBean JwtAuthFilter jwtAuthFilter;
    @MockitoBean OAuth2SuccessHandler oAuth2SuccessHandler;

    @BeforeEach
    void allowFilterChainToProceed() throws Exception {
        doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    private User actor() {
        return User.builder().id(1L).email("worker@test.ma").role(Role.WORKER).build();
    }

    @Test
    void ping_returnsOnlineStatus() throws Exception {
        mockMvc.perform(get("/api/v1/sync/status").with(SecurityMockMvcRequestPostProcessors.user(actor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("online"))
                .andExpect(jsonPath("$.server").value("BricoPro API"));
    }

    @Test
    void syncActions_acceptTask_dispatchesCorrectly() throws Exception {
        when(taskService.acceptTask(any(), eq(5L))).thenReturn(new TaskResponse());

        String body = """
            {"actions":[{"localId":"local-1","type":"ACCEPT_TASK","payload":{"taskId":5},"timestamp":123}]}
        """;

        mockMvc.perform(post("/api/v1/sync/actions")
                        .with(SecurityMockMvcRequestPostProcessors.user(actor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.succeeded").value(1))
                .andExpect(jsonPath("$.failed").value(0))
                .andExpect(jsonPath("$.results[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.results[0].localId").value("local-1"));

        verify(taskService).acceptTask(any(), eq(5L));
    }

    @Test
    void syncActions_updateTaskStatus_withAgreedPriceAndReason() throws Exception {
        when(taskService.updateStatus(any(), eq(7L), any())).thenReturn(new TaskResponse());

        String body = """
            {"actions":[{"localId":"local-2","type":"UPDATE_TASK_STATUS",
             "payload":{"taskId":7,"status":"COMPLETED","agreedPrice":"250.00"},"timestamp":123}]}
        """;

        mockMvc.perform(post("/api/v1/sync/actions")
                        .with(SecurityMockMvcRequestPostProcessors.user(actor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("SUCCESS"));

        verify(taskService).updateStatus(any(), eq(7L), argThat(req ->
                req.getStatus() == TaskStatus.COMPLETED
                        && req.getAgreedPrice().compareTo(new java.math.BigDecimal("250.00")) == 0));
    }

    @Test
    void syncActions_sendMessage_dispatchesCorrectly() throws Exception {
        String body = """
            {"actions":[{"localId":"local-3","type":"SEND_MESSAGE",
             "payload":{"conversationId":9,"content":"On my way"},"timestamp":123}]}
        """;

        mockMvc.perform(post("/api/v1/sync/actions")
                        .with(SecurityMockMvcRequestPostProcessors.user(actor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("SUCCESS"));

        verify(messagingService).sendMessage(eq(1L), eq(9L), argThat(req -> req.getContent().equals("On my way")));
    }

    @Test
    void syncActions_markMessagesRead_dispatchesCorrectly() throws Exception {
        String body = """
            {"actions":[{"localId":"local-4","type":"MARK_MESSAGES_READ",
             "payload":{"conversationId":9},"timestamp":123}]}
        """;

        mockMvc.perform(post("/api/v1/sync/actions")
                        .with(SecurityMockMvcRequestPostProcessors.user(actor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("SUCCESS"));

        verify(messagingService).markRead(9L, 1L);
    }

    @Test
    void syncActions_unknownType_reportsFailureWithoutAbortingOthers() throws Exception {
        when(taskService.acceptTask(any(), eq(5L))).thenReturn(new TaskResponse());

        String body = """
            {"actions":[
                {"localId":"local-bad","type":"DO_A_BACKFLIP","payload":{},"timestamp":123},
                {"localId":"local-good","type":"ACCEPT_TASK","payload":{"taskId":5},"timestamp":124}
            ]}
        """;

        mockMvc.perform(post("/api/v1/sync/actions")
                        .with(SecurityMockMvcRequestPostProcessors.user(actor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.succeeded").value(1))
                .andExpect(jsonPath("$.failed").value(1))
                .andExpect(jsonPath("$.results[0].status").value("FAILED"))
                .andExpect(jsonPath("$.results[1].status").value("SUCCESS"));

        verify(taskService).acceptTask(any(), eq(5L));
    }

    @Test
    void syncActions_missingRequiredField_reportsFailure() throws Exception {
        String body = """
            {"actions":[{"localId":"local-5","type":"ACCEPT_TASK","payload":{},"timestamp":123}]}
        """;

        mockMvc.perform(post("/api/v1/sync/actions")
                        .with(SecurityMockMvcRequestPostProcessors.user(actor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failed").value(1))
                .andExpect(jsonPath("$.results[0].error").value("Missing field: taskId"));

        verifyNoInteractions(taskService);
    }

    @Test
    void syncActions_emptyActionsList_returnsZeroCounts() throws Exception {
        String body = """
            {"actions":[]}
        """;

        mockMvc.perform(post("/api/v1/sync/actions")
                        .with(SecurityMockMvcRequestPostProcessors.user(actor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.succeeded").value(0))
                .andExpect(jsonPath("$.failed").value(0))
                .andExpect(jsonPath("$.results").isEmpty());
    }
}