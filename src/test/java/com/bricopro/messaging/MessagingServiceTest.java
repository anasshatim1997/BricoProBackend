package com.bricopro.messaging;

import com.bricopro.messaging.dto.MessagingDtos.*;
import com.bricopro.messaging.entity.Conversation;
import com.bricopro.messaging.entity.Message;
import com.bricopro.messaging.entity.Message.MessageType;
import com.bricopro.messaging.repository.ConversationRepository;
import com.bricopro.messaging.repository.MessageRepository;
import com.bricopro.messaging.service.MessagingService;
import com.bricopro.notification.service.CommunicationService;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Role;
import com.bricopro.user.entity.User.Status;
import com.bricopro.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessagingService")
class MessagingServiceTest {

    @Mock ConversationRepository conversationRepository;
    @Mock MessageRepository messageRepository;
    @Mock UserRepository userRepository;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock CommunicationService communicationService;

    @InjectMocks MessagingService messagingService;

    private User client;
    private User worker;
    private Conversation conversation;

    @BeforeEach
    void setup() {
        client = User.builder().id(1L).firstName("Leila").lastName("Bensaid")
                .email("leila@test.ma").role(Role.CLIENT).status(Status.ACTIVE).build();

        worker = User.builder().id(2L).firstName("Mehdi").lastName("Harrak")
                .email("mehdi@test.ma").role(Role.WORKER).status(Status.ACTIVE).build();

        conversation = Conversation.builder()
                .id(100L).client(client).worker(worker).taskId(5L)
                .createdAt(LocalDateTime.now()).build();
    }

    // ─── GET OR CREATE CONVERSATION ───────────────────────────────────────────

    @Nested
    @DisplayName("getOrCreateConversation()")
    class GetOrCreate {

        @Test
        @DisplayName("returns existing conversation if found")
        void returnsExisting() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(client));
            when(userRepository.findById(2L)).thenReturn(Optional.of(worker));
            when(conversationRepository.findByClientIdAndWorkerIdAndTaskId(1L, 2L, 5L))
                    .thenReturn(Optional.of(conversation));
            when(messageRepository.countByConversationIdAndIsReadFalseAndSenderIdNot(100L, 1L))
                    .thenReturn(0L);

            ConversationResponse res = messagingService.getOrCreateConversation(1L, 2L, 5L);
            assertThat(res.getId()).isEqualTo(100L);
            verify(conversationRepository, never()).save(any());
        }

        @Test
        @DisplayName("creates new conversation when not found")
        void createsNew() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(client));
            when(userRepository.findById(2L)).thenReturn(Optional.of(worker));
            when(conversationRepository.findByClientIdAndWorkerIdAndTaskId(1L, 2L, 5L))
                    .thenReturn(Optional.empty());
            Conversation newConv = Conversation.builder()
                    .id(200L).client(client).worker(worker).taskId(5L)
                    .createdAt(LocalDateTime.now()).build();
            when(conversationRepository.save(any())).thenReturn(newConv);
            when(messageRepository.countByConversationIdAndIsReadFalseAndSenderIdNot(200L, 1L))
                    .thenReturn(0L);

            ConversationResponse res = messagingService.getOrCreateConversation(1L, 2L, 5L);
            assertThat(res.getId()).isEqualTo(200L);
            verify(conversationRepository).save(any());
        }

        @Test
        @DisplayName("correctly assigns client and worker roles regardless of caller order")
        void correctlyAssignsRoles() {
            // Worker initiates conversation (calls with workerId=2 first)
            when(userRepository.findById(2L)).thenReturn(Optional.of(worker));
            when(userRepository.findById(1L)).thenReturn(Optional.of(client));
            when(conversationRepository.findByClientIdAndWorkerIdAndTaskId(1L, 2L, 5L))
                    .thenReturn(Optional.empty());
            when(conversationRepository.save(any())).thenReturn(conversation);
            when(messageRepository.countByConversationIdAndIsReadFalseAndSenderIdNot(100L, 2L))
                    .thenReturn(0L);

            ConversationResponse res = messagingService.getOrCreateConversation(2L, 1L, 5L);
            assertThat(res.getWorkerId()).isEqualTo(2L);
            assertThat(res.getClientId()).isEqualTo(1L);
        }
    }

    // ─── SEND MESSAGE ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sendMessage()")
    class SendMessage {

        @Test
        @DisplayName("saves message and sends via WebSocket to recipient")
        void savesAndBroadcasts() {
            when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));
            when(userRepository.findById(1L)).thenReturn(Optional.of(client));

            Message saved = Message.builder()
                    .id(1L).conversation(conversation).sender(client)
                    .content("Hello!").messageType(MessageType.TEXT)
                    .isRead(false).createdAt(LocalDateTime.now()).build();
            when(messageRepository.save(any())).thenReturn(saved);
            when(messageRepository.countByConversationIdAndIsReadFalseAndSenderIdNot(100L, 2L))
                    .thenReturn(1L);

            SendMessageRequest req = new SendMessageRequest();
            req.setContent("Hello!");

            MessageResponse res = messagingService.sendMessage(1L, 100L, req);

            assertThat(res.getContent()).isEqualTo("Hello!");
            verify(messagingTemplate).convertAndSendToUser(
                    eq("2"), eq("/queue/messages"), any(MessageResponse.class));
        }

        @Test
        @DisplayName("sends email notification on first unread message")
        void sendsEmailOnFirstUnread() {
            when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));
            when(userRepository.findById(1L)).thenReturn(Optional.of(client));

            Message saved = Message.builder().id(1L).conversation(conversation).sender(client)
                    .content("Need help!").messageType(MessageType.TEXT)
                    .isRead(false).createdAt(LocalDateTime.now()).build();
            when(messageRepository.save(any())).thenReturn(saved);
            when(messageRepository.countByConversationIdAndIsReadFalseAndSenderIdNot(100L, 2L))
                    .thenReturn(1L);

            SendMessageRequest req = new SendMessageRequest();
            req.setContent("Need help!");
            messagingService.sendMessage(1L, 100L, req);

            verify(communicationService).sendNewMessageNotificationEmail(
                    eq("mehdi@test.ma"), eq("Mehdi"), anyString());
        }

        @Test
        @DisplayName("does not send repeat email when many unread messages already exist")
        void noRepeatEmail() {
            when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));
            when(userRepository.findById(1L)).thenReturn(Optional.of(client));

            Message saved = Message.builder().id(10L).conversation(conversation).sender(client)
                    .content("Are you there?").messageType(MessageType.TEXT)
                    .isRead(false).createdAt(LocalDateTime.now()).build();
            when(messageRepository.save(any())).thenReturn(saved);
            when(messageRepository.countByConversationIdAndIsReadFalseAndSenderIdNot(100L, 2L))
                    .thenReturn(5L); // already 5 unread

            SendMessageRequest req = new SendMessageRequest();
            req.setContent("Are you there?");
            messagingService.sendMessage(1L, 100L, req);

            verify(communicationService, never()).sendNewMessageNotificationEmail(any(), any(), any());
        }

        @Test
        @DisplayName("throws SecurityException when sender is not a participant")
        void nonParticipantThrows() {
            User outsider = User.builder().id(99L).role(Role.CLIENT).build();

            when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));
            when(userRepository.findById(99L)).thenReturn(Optional.of(outsider));
            when(messageRepository.save(any())).thenAnswer(inv -> {
                Message m = inv.getArgument(0);
                m = Message.builder().id(1L).conversation(conversation).sender(outsider)
                        .content("intrusion").messageType(MessageType.TEXT)
                        .isRead(false).createdAt(LocalDateTime.now()).build();
                return m;
            });

            SendMessageRequest req = new SendMessageRequest();
            req.setContent("intrusion");

            // SecurityException is thrown inside sendMessage after save but during participant validation
            // The service calls validateParticipant which throws SecurityException
            // We check it's thrown here at conversation lookup
            assertThatThrownBy(() -> messagingService.sendMessage(99L, 100L, req))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("participant");
        }

        @Test
        @DisplayName("defaults message type to TEXT when not specified")
        void defaultsToText() {
            when(conversationRepository.findById(100L)).thenReturn(Optional.of(conversation));
            when(userRepository.findById(1L)).thenReturn(Optional.of(client));

            Message saved = Message.builder().id(1L).conversation(conversation).sender(client)
                    .content("Hi").messageType(MessageType.TEXT).isRead(false)
                    .createdAt(LocalDateTime.now()).build();
            when(messageRepository.save(any())).thenReturn(saved);
            when(messageRepository.countByConversationIdAndIsReadFalseAndSenderIdNot(anyLong(), anyLong()))
                    .thenReturn(0L);

            SendMessageRequest req = new SendMessageRequest();
            req.setContent("Hi");
            req.setMessageType(null); // not specified

            MessageResponse res = messagingService.sendMessage(1L, 100L, req);
            assertThat(res.getMessageType()).isEqualTo("TEXT");
        }
    }

    // ─── MARK READ ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("markRead() calls repository bulk update")
    void markRead() {
        messagingService.markRead(100L, 1L);
        verify(messageRepository).markAllReadInConversation(100L, 1L);
    }

    // ─── GET MESSAGES ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getMessages() returns paginated messages for a conversation")
    void getMessages() {
        PageRequest pg = PageRequest.of(0, 20);
        Message msg = Message.builder().id(1L).conversation(conversation).sender(client)
                .content("Test").messageType(MessageType.TEXT).isRead(true)
                .createdAt(LocalDateTime.now()).build();

        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(100L, pg))
                .thenReturn(new PageImpl<>(List.of(msg)));

        Page<MessageResponse> res = messagingService.getMessages(100L, pg);
        assertThat(res.getTotalElements()).isEqualTo(1);
    }

    // ─── GET MY CONVERSATIONS ────────────────────────────────────────────────

    @Test
    @DisplayName("getMyConversations() returns paginated conversations for user")
    void getMyConversations() {
        PageRequest pg = PageRequest.of(0, 10);
        when(conversationRepository.findByUserId(1L, pg))
                .thenReturn(new PageImpl<>(List.of(conversation)));
        when(messageRepository.countByConversationIdAndIsReadFalseAndSenderIdNot(100L, 1L))
                .thenReturn(2L);

        Page<ConversationResponse> res = messagingService.getMyConversations(1L, pg);
        assertThat(res.getTotalElements()).isEqualTo(1);
        assertThat(res.getContent().get(0).getUnreadCount()).isEqualTo(2L);
    }
}
