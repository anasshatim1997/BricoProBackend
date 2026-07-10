package com.bricopro.bidding.controller;

import com.bricopro.bidding.dto.BidDtos;
import com.bricopro.bidding.service.IBiddingService;
import com.bricopro.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bids")
@RequiredArgsConstructor
public class BiddingController {

    private final IBiddingService biddingService;

    @PostMapping
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<BidDtos.BidResponse> createBid(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid BidDtos.CreateBidRequest request) {
        return ResponseEntity.ok(biddingService.createBid(user.getId(), request));
    }

    @PutMapping("/{bidId}")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<BidDtos.BidResponse> updateBid(
            @AuthenticationPrincipal User user,
            @PathVariable Long bidId,
            @RequestBody @Valid BidDtos.UpdateBidRequest request) {
        return ResponseEntity.ok(biddingService.updateBid(bidId, user.getId(), request));
    }

    @DeleteMapping("/{bidId}")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<Void> withdrawBid(
            @AuthenticationPrincipal User user,
            @PathVariable Long bidId) {
        biddingService.withdrawBid(bidId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{bidId}/accept")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Void> acceptBid(
            @AuthenticationPrincipal User user,
            @PathVariable Long bidId) {
        biddingService.acceptBid(bidId, user.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{bidId}/reject")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Void> rejectBid(
            @AuthenticationPrincipal User user,
            @PathVariable Long bidId) {
        biddingService.rejectBid(bidId, user.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<BidDtos.BidResponse>> getBidsForTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(biddingService.getBidsForTask(taskId));
    }

    @GetMapping("/worker")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<List<BidDtos.BidResponse>> getMyBids(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(biddingService.getBidsByWorker(user.getId()));
    }

    @GetMapping("/{bidId}")
    public ResponseEntity<BidDtos.BidResponse> getBid(@PathVariable Long bidId) {
        return ResponseEntity.ok(biddingService.getBid(bidId));
    }

    @PostMapping("/{bidId}/counter")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<BidDtos.BidResponse> counterBid(
            @AuthenticationPrincipal User user,
            @PathVariable Long bidId,
            @RequestBody @Valid BidDtos.CounterBidRequest request) {
        return ResponseEntity.ok(biddingService.counterBid(bidId, user.getId(), request));
    }

    @PostMapping("/{bidId}/revise")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<BidDtos.BidResponse> reviseBid(
            @AuthenticationPrincipal User user,
            @PathVariable Long bidId,
            @RequestBody @Valid BidDtos.ReviseBidRequest request) {
        return ResponseEntity.ok(biddingService.reviseBid(bidId, user.getId(), request));
    }

    @GetMapping("/{bidId}/history")
    public ResponseEntity<List<BidDtos.BidResponse>> getBidHistory(@PathVariable Long bidId) {
        return ResponseEntity.ok(biddingService.getBidHistory(bidId));
    }
}
