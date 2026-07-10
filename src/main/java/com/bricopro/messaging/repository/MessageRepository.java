package com.bricopro.messaging.repository;

import com.bricopro.messaging.entity.Message;
import com.bricopro.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);
    long countByConversationIdAndIsReadFalseAndSenderIdNot(Long conversationId, Long userId);


    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.client = :user OR m.conversation.worker = :user AND m.isRead = false")
    long countUnreadByUser(@Param("user") User user);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.conversation.id = :convId AND m.sender.id != :userId")
    void markAllReadInConversation(Long convId, Long userId);
}
