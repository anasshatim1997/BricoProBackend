package com.bricopro.messaging;

import com.bricopro.bidding.entity.Bid;
import com.bricopro.messaging.websocket.WebSocketNotifier;
import com.bricopro.task.entity.Task;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketNotifier")
class WebSocketNotifierTest {

    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock ObjectMapper objectMapper;

    @InjectMocks WebSocketNotifier notifier;

    @Nested
    @DisplayName("notifyNewBid()")
    class NotifyNewBid {

        @Test
        @DisplayName("publishes the serialized bid to the task's bid topic")
        void publishesToCorrectTopic() throws Exception {
            Bid bid = Bid.builder().id(1L).taskId(7L).build();
            when(objectMapper.writeValueAsString(bid)).thenReturn("{\"id\":1}");

            notifier.notifyNewBid(bid);

            verify(messagingTemplate).convertAndSend("/topic/task/7/bids", "{\"id\":1}");
        }

        @Test
        @DisplayName("REGRESSION: does not propagate a serialization failure, and does not crash the caller")
        void serializationFailureDoesNotThrow() throws Exception {
            Bid bid = Bid.builder().id(1L).taskId(7L).build();
            when(objectMapper.writeValueAsString(bid)).thenThrow(new RuntimeException("boom"));

            assertThatCode(() -> notifier.notifyNewBid(bid)).doesNotThrowAnyException();

            verify(messagingTemplate, never()).convertAndSend(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("notifyMatch()")
    class NotifyMatch {

        @Test
        @DisplayName("sends the serialized task to the worker's personal match queue")
        void sendsToCorrectUserQueue() throws Exception {
            Task task = Task.builder().id(9L).build();
            when(objectMapper.writeValueAsString(task)).thenReturn("{\"id\":9}");

            notifier.notifyMatch(42L, task);

            verify(messagingTemplate).convertAndSendToUser("42", "/queue/matches", "{\"id\":9}");
        }

        @Test
        @DisplayName("REGRESSION: does not propagate a serialization failure, and does not crash the caller")
        void serializationFailureDoesNotThrow() throws Exception {
            Task task = Task.builder().id(9L).build();
            when(objectMapper.writeValueAsString(task)).thenThrow(new RuntimeException("boom"));

            assertThatCode(() -> notifier.notifyMatch(42L, task)).doesNotThrowAnyException();

            verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
        }
    }
}
