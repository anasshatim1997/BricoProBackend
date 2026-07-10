package com.bricopro.bidding.repository;

import com.bricopro.bidding.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long> {

    List<Bid> findByTaskId(Long taskId);

    List<Bid> findByTaskIdAndStatus(Long taskId, Bid.BidStatus status);

    List<Bid> findByWorkerId(Long workerId);

    Optional<Bid> findByTaskIdAndWorkerId(Long taskId, Long workerId);

    List<Bid> findByParentBidId(Long parentBidId);

    @Modifying
    @Transactional
    @Query("UPDATE Bid b SET b.status = :status WHERE b.id = :id AND b.status = 'PENDING'")
    void updateStatusIfPending(@Param("id") Long id, @Param("status") Bid.BidStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE Bid b SET b.status = 'EXPIRED' WHERE b.status = 'PENDING' AND b.expiresAt < CURRENT_TIMESTAMP")
    void expirePendingBids();
}