package com.bricopro.messaging.websocket;

import com.bricopro.bidding.entity.Bid;
import com.bricopro.task.entity.Task;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotifier {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public void notifyNewBid(Bid bid) {
        try {
            String payload = objectMapper.writeValueAsString(bid);
            messagingTemplate.convertAndSend("/topic/task/" + bid.getTaskId() + "/bids", payload);
        } catch (Exception e) {
            log.error("Failed to send new-bid WebSocket notification for taskId={}", bid.getTaskId(), e);
        }
    }

    public void notifyMatch(Long workerId, Task task) {
        try {
            String payload = objectMapper.writeValueAsString(task);
            messagingTemplate.convertAndSendToUser(workerId.toString(), "/queue/matches", payload);
        } catch (Exception e) {
            log.error("Failed to send match WebSocket notification for workerId={}", workerId, e);
        }
    }
}