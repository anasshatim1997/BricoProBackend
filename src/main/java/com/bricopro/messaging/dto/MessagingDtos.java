package com.bricopro.messaging.dto;

import com.bricopro.messaging.entity.Message.MessageType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

public class MessagingDtos {

    @Data
    @Schema(description = "Request payload for: Send Message.")
    public static class SendMessageRequest {
        @NotBlank
        @Schema(description = "Content", example = "example")
        private String content;
        @Schema(description = "Media Url", example = "example")
        private String mediaUrl;
        @Schema(description = "Human-readable response message", example = "value")
        private MessageType messageType;
    }

    @Data
    @Schema(description = "Response body returned by: Conversation.")
    public static class ConversationResponse {
        @Schema(description = "Unique identifier", example = "1")
        private Long id;
        @Schema(description = "ID of the related task", example = "1")
        private Long taskId;
        @Schema(description = "ID of the requesting client", example = "1")
        private Long clientId;
        @Schema(description = "ID of the assigned or target worker", example = "1")
        private Long workerId;
        @Schema(description = "Other User Name", example = "example")
        private String otherUserName;
        @Schema(description = "Unread Count", example = "value")
        private long unreadCount;
        @Schema(description = "ISO-8601 timestamp when the record was created", example = "2025-06-15")
        private LocalDateTime createdAt;
    }

    @Data
    @Schema(description = "Response body returned by: Message.")
    public static class MessageResponse {
        @Schema(description = "Unique identifier", example = "1")
        private Long id;
        @Schema(description = "Conversation Id", example = "1")
        private Long conversationId;
        @Schema(description = "Sender Id", example = "1")
        private Long senderId;
        @Schema(description = "Sender Name", example = "example")
        private String senderName;
        @Schema(description = "Content", example = "example")
        private String content;
        @Schema(description = "Media Url", example = "example")
        private String mediaUrl;
        @Schema(description = "Human-readable response message", example = "example")
        private String messageType;
        @Schema(description = "Is Read", example = "false")
        private boolean isRead;
        @Schema(description = "ISO-8601 timestamp when the record was created", example = "2025-06-15")
        private LocalDateTime createdAt;
    }
}
