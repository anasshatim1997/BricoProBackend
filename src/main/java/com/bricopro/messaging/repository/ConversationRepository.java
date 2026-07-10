package com.bricopro.messaging.repository;

import com.bricopro.messaging.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByClientIdAndWorkerIdAndTaskId(Long clientId, Long workerId, Long taskId);

    @Query("SELECT c FROM Conversation c WHERE c.client.id = :userId OR c.worker.id = :userId ORDER BY c.createdAt DESC")
    Page<Conversation> findByUserId(Long userId, Pageable pageable);
}
