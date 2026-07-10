package com.bricopro.bidding.service;

import com.bricopro.bidding.dto.BidDtos;
import java.util.List;

public interface IBiddingService {

    BidDtos.BidResponse createBid(Long workerId, BidDtos.CreateBidRequest request);

    BidDtos.BidResponse updateBid(Long bidId, Long workerId, BidDtos.UpdateBidRequest request);

    void withdrawBid(Long bidId, Long workerId);

    void acceptBid(Long bidId, Long clientId);

    void rejectBid(Long bidId, Long clientId);

    List<BidDtos.BidResponse> getBidsForTask(Long taskId);

    List<BidDtos.BidResponse> getBidsByWorker(Long workerId);

    BidDtos.BidResponse getBid(Long bidId);

    void expirePendingBids();

    BidDtos.BidResponse counterBid(Long bidId, Long clientId, BidDtos.CounterBidRequest request);

    BidDtos.BidResponse reviseBid(Long bidId, Long workerId, BidDtos.ReviseBidRequest request);

    List<BidDtos.BidResponse> getBidHistory(Long bidId);
}