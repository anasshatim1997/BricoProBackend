package com.bricopro.bidding.mapper;

import com.bricopro.bidding.dto.BidDtos;
import com.bricopro.bidding.entity.Bid;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.WorkerProfile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BidMapper {

    public Bid toEntity(BidDtos.CreateBidRequest request, Long workerId) {
        return Bid.builder()
                .taskId(request.getTaskId())
                .workerId(workerId)
                .amount(request.getAmount())
                .message(request.getMessage())
                .estimatedDurationHours(request.getEstimatedDurationHours())
                .status(Bid.BidStatus.PENDING)
                .build();
    }

    public BidDtos.BidResponse toResponse(Bid bid, User worker) {
        String name = null;
        Double rating = null;
        if (worker != null) {
            name = worker.getFirstName() + " " + worker.getLastName();
            WorkerProfile profile = worker.getWorkerProfile();
            if (profile != null) {
                rating = profile.getAverageRating().doubleValue();
            }
        }
        return BidDtos.BidResponse.builder()
                .id(bid.getId())
                .taskId(bid.getTaskId())
                .workerId(bid.getWorkerId())
                .amount(bid.getAmount())
                .message(bid.getMessage())
                .estimatedDurationHours(bid.getEstimatedDurationHours())
                .status(bid.getStatus().name())
                .createdAt(bid.getCreatedAt())
                .expiresAt(bid.getExpiresAt())
                .workerName(name)
                .workerRating(rating)
                .parentBidId(bid.getParentBidId())
                .build();
    }

    public BidDtos.BidResponse toResponseWithChildren(Bid bid, User worker, List<Bid> children) {
        BidDtos.BidResponse response = toResponse(bid, worker);
        if (children != null && !children.isEmpty()) {
            response.setChildren(children.stream()
                    .map(child -> toResponse(child, worker))
                    .collect(Collectors.toList()));
        }
        return response;
    }
}