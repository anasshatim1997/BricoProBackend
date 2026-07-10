package com.bricopro.messaging.service;

import com.bricopro.messaging.dto.MessagingDtos.*;
import com.bricopro.messaging.entity.Conversation;
import com.bricopro.messaging.entity.Message;
import com.bricopro.messaging.entity.Message.MessageType;
import com.bricopro.messaging.repository.ConversationRepository;
import com.bricopro.messaging.repository.MessageRepository;
import com.bricopro.notification.service.CommunicationService;
import com.bricopro.user.entity.User;
import com.bricopro.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Tag(name = "Messaging Service", description = "Business logic for Messaging Service")
@Service
@RequiredArgsConstructor
public class MessagingService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository      messageRepository;
    private final UserRepository         userRepository;
    private final SimpMessagingTemplate  messagingTemplate;
    private final CommunicationService   communicationService;

    @Transactional
    public ConversationResponse getOrCreateConversation(Long currentUserId, Long otherUserId, Long taskId) {
        User current = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        User other = userRepository.findById(otherUserId)
                .orElseThrow(() -> new IllegalArgumentException("Other user not found"));

        Long clientId = current.getRole().name().equals("CLIENT") ? currentUserId : otherUserId;
        Long workerId = current.getRole().name().equals("WORKER") ? currentUserId : otherUserId;

        Conversation conv = conversationRepository
                .findByClientIdAndWorkerIdAndTaskId(clientId, workerId, taskId)
                .orElseGet(() -> conversationRepository.save(
                        Conversation.builder()
                                .client(current.getRole().name().equals("CLIENT") ? current : other)
                                .worker(current.getRole().name().equals("WORKER") ? current : other)
                                .taskId(taskId)
                                .build()
                ));

        return toConversationResponse(conv, currentUserId);
    }

    public Page<ConversationResponse> getMyConversations(Long userId, Pageable pageable) {
        return conversationRepository.findByUserId(userId, pageable)
                .map(c -> toConversationResponse(c, userId));
    }

    public Page<MessageResponse> getMessages(Long conversationId, Pageable pageable) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, pageable)
                .map(this::toMessageResponse);
    }

    @Transactional
    public MessageResponse sendMessage(Long senderId, Long conversationId, SendMessageRequest req) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        validateParticipant(senderId, conv);

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));

        Message msg = Message.builder()
                .conversation(conv)
                .sender(sender)
                .content(req.getContent())
                .mediaUrl(req.getMediaUrl())
                .messageType(req.getMessageType() != null ? req.getMessageType() : MessageType.TEXT)
                .build();

        msg = messageRepository.save(msg);
        MessageResponse response = toMessageResponse(msg);

        User recipient = senderId.equals(conv.getClient().getId())
                ? conv.getWorker()
                : conv.getClient();

        messagingTemplate.convertAndSendToUser(
                recipient.getId().toString(),
                "/queue/messages",
                response
        );

        long unreadCount = messageRepository
                .countByConversationIdAndIsReadFalseAndSenderIdNot(conversationId, recipient.getId());

        if (unreadCount <= 1 && recipient.getEmail() != null) {
            communicationService.sendNewMessageNotificationEmail(
                    recipient.getEmail(),
                    recipient.getFirstName(),
                    sender.getFirstName() + " " + sender.getLastName()
            );
        }

        return response;
    }

    @Transactional
    public void markRead(Long conversationId, Long userId) {
        messageRepository.markAllReadInConversation(conversationId, userId);
    }

    private void validateParticipant(Long userId, Conversation conv) {
        if (!userId.equals(conv.getClient().getId()) && !userId.equals(conv.getWorker().getId()))
            throw new SecurityException("You are not a participant of this conversation");
    }

    private ConversationResponse toConversationResponse(Conversation c, Long currentUserId) {
        ConversationResponse dto = new ConversationResponse();
        dto.setId(c.getId());
        dto.setTaskId(c.getTaskId());
        dto.setClientId(c.getClient().getId());
        dto.setWorkerId(c.getWorker().getId());
        dto.setOtherUserName(currentUserId.equals(c.getClient().getId())
                ? c.getWorker().getFirstName() + " " + c.getWorker().getLastName()
                : c.getClient().getFirstName() + " " + c.getClient().getLastName());
        dto.setUnreadCount(messageRepository
                .countByConversationIdAndIsReadFalseAndSenderIdNot(c.getId(), currentUserId));
        dto.setCreatedAt(c.getCreatedAt());
        return dto;
    }

    private MessageResponse toMessageResponse(Message m) {
        MessageResponse dto = new MessageResponse();
        dto.setId(m.getId());
        dto.setConversationId(m.getConversation().getId());
        dto.setSenderId(m.getSender().getId());
        dto.setSenderName(m.getSender().getFirstName() + " " + m.getSender().getLastName());
        dto.setContent(m.getContent());
        dto.setMediaUrl(m.getMediaUrl());
        dto.setMessageType(m.getMessageType().name());
        dto.setRead(m.isRead());
        dto.setCreatedAt(m.getCreatedAt());
        return dto;
    }
}