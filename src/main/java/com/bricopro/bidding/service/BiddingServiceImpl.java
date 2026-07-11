package com.bricopro.bidding.service;

import com.bricopro.bidding.dto.BidDtos;
import com.bricopro.bidding.entity.Bid;
import com.bricopro.bidding.mapper.BidMapper;
import com.bricopro.bidding.repository.BidRepository;
import com.bricopro.task.entity.Task;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BiddingServiceImpl implements IBiddingService {

    private final BidRepository bidRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final BidMapper bidMapper;
    private final com.bricopro.user.service.WorkerSnapshotService workerSnapshotService;

    @Override
    @Transactional
    public BidDtos.BidResponse createBid(Long workerId, BidDtos.CreateBidRequest request) {
        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new RuntimeException("Task not found"));
        if (!Boolean.TRUE.equals(task.getBiddingEnabled())) {
            throw new RuntimeException("Bidding is not enabled for this task");
        }
        if (task.getBiddingDeadline() != null && task.getBiddingDeadline().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Bidding deadline has passed");
        }
        if (bidRepository.findByTaskIdAndWorkerId(task.getId(), workerId).isPresent()) {
            throw new RuntimeException("You have already placed a bid on this task");
        }
        Bid bid = bidMapper.toEntity(request, workerId);
        bid.setExpiresAt(task.getBiddingDeadline());
        bid = bidRepository.save(bid);
        User worker = userRepository.findById(workerId).orElse(null);
        return bidMapper.toResponse(bid, worker);
    }

    @Override
    @Transactional
    public BidDtos.BidResponse updateBid(Long bidId, Long workerId, BidDtos.UpdateBidRequest request) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new RuntimeException("Bid not found"));
        if (!bid.getWorkerId().equals(workerId)) {
            throw new RuntimeException("Not authorized to update this bid");
        }
        if (bid.getStatus() != Bid.BidStatus.PENDING) {
            throw new RuntimeException("Cannot update a bid that is not pending");
        }
        bid.setAmount(request.getAmount());
        bid.setMessage(request.getMessage());
        bid.setEstimatedDurationHours(request.getEstimatedDurationHours());
        bid = bidRepository.save(bid);
        User worker = userRepository.findById(workerId).orElse(null);
        return bidMapper.toResponse(bid, worker);
    }

    @Override
    @Transactional
    public void withdrawBid(Long bidId, Long workerId) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new RuntimeException("Bid not found"));
        if (!bid.getWorkerId().equals(workerId)) {
            throw new RuntimeException("Not authorized to withdraw this bid");
        }
        if (bid.getStatus() != Bid.BidStatus.PENDING) {
            throw new RuntimeException("Cannot withdraw a bid that is not pending");
        }
        bid.setStatus(Bid.BidStatus.WITHDRAWN);
        bidRepository.save(bid);
    }

    @Override
    @Transactional
    public void acceptBid(Long bidId, Long clientId) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new RuntimeException("Bid not found"));
        Task task = taskRepository.findById(bid.getTaskId())
                .orElseThrow(() -> new RuntimeException("Task not found"));
        if (!task.getClient().getId().equals(clientId)) {
            throw new RuntimeException("Only the task client can accept bids");
        }
        if (bid.getStatus() != Bid.BidStatus.PENDING) {
            throw new RuntimeException("Bid is not pending");
        }
        if (task.getStatus() != Task.TaskStatus.PENDING && task.getStatus() != Task.TaskStatus.SEARCHING) {
            throw new RuntimeException("This task is no longer open for new assignments (current status: " + task.getStatus() + ")");
        }
        bid.setStatus(Bid.BidStatus.ACCEPTED);
        bidRepository.save(bid);
        User worker = userRepository.findById(bid.getWorkerId())
                .orElseThrow(() -> new RuntimeException("Worker not found"));
        task.setWorker(worker);
        task.setStatus(Task.TaskStatus.CONFIRMED);
        taskRepository.save(task);
        workerSnapshotService.captureOnAssignment(worker.getId(), task.getId());
        bidRepository.rejectOtherPendingBids(task.getId(), bid.getId(), Bid.BidStatus.REJECTED);
    }

    @Override
    @Transactional
    public void rejectBid(Long bidId, Long clientId) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new RuntimeException("Bid not found"));
        Task task = taskRepository.findById(bid.getTaskId())
                .orElseThrow(() -> new RuntimeException("Task not found"));
        if (!task.getClient().getId().equals(clientId)) {
            throw new RuntimeException("Only the task client can reject bids");
        }
        if (bid.getStatus() != Bid.BidStatus.PENDING) {
            throw new RuntimeException("Bid is not pending");
        }
        bid.setStatus(Bid.BidStatus.REJECTED);
        bidRepository.save(bid);
    }

    @Override
    public List<BidDtos.BidResponse> getBidsForTask(Long taskId) {
        List<Bid> bids = bidRepository.findByTaskId(taskId);
        return bids.stream()
                .map(bid -> {
                    User worker = userRepository.findById(bid.getWorkerId()).orElse(null);
                    return bidMapper.toResponse(bid, worker);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<BidDtos.BidResponse> getBidsByWorker(Long workerId) {
        List<Bid> bids = bidRepository.findByWorkerId(workerId);
        return bids.stream()
                .map(bid -> {
                    User worker = userRepository.findById(bid.getWorkerId()).orElse(null);
                    return bidMapper.toResponse(bid, worker);
                })
                .collect(Collectors.toList());
    }

    @Override
    public BidDtos.BidResponse getBid(Long bidId) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new RuntimeException("Bid not found"));
        User worker = userRepository.findById(bid.getWorkerId()).orElse(null);
        return bidMapper.toResponse(bid, worker);
    }

    @Override
    @Transactional
    public void expirePendingBids() {
        bidRepository.expirePendingBids();
    }

    @Override
    @Transactional
    public BidDtos.BidResponse counterBid(Long bidId, Long clientId, BidDtos.CounterBidRequest request) {
        Bid original = bidRepository.findById(bidId)
                .orElseThrow(() -> new RuntimeException("Bid not found"));
        Task task = taskRepository.findById(original.getTaskId())
                .orElseThrow(() -> new RuntimeException("Task not found"));
        if (!task.getClient().getId().equals(clientId)) {
            throw new RuntimeException("Only the client can counter a bid");
        }
        if (original.getStatus() != Bid.BidStatus.PENDING) {
            throw new RuntimeException("Only pending bids can be countered");
        }

        Bid counter = Bid.builder()
                .taskId(original.getTaskId())
                .workerId(original.getWorkerId())
                .amount(request.getAmount())
                .message(request.getMessage())
                .estimatedDurationHours(original.getEstimatedDurationHours())
                .status(Bid.BidStatus.PENDING)
                .parentBidId(original.getId())
                .expiresAt(original.getExpiresAt())
                .build();
        counter = bidRepository.save(counter);

        original.setStatus(Bid.BidStatus.COUNTERED);
        bidRepository.save(original);

        User worker = userRepository.findById(original.getWorkerId()).orElse(null);
        return bidMapper.toResponse(counter, worker);
    }

    @Override
    @Transactional
    public BidDtos.BidResponse reviseBid(Long bidId, Long workerId, BidDtos.ReviseBidRequest request) {
        Bid original = bidRepository.findById(bidId)
                .orElseThrow(() -> new RuntimeException("Bid not found"));
        if (!original.getWorkerId().equals(workerId)) {
            throw new RuntimeException("Only the worker can revise their bid");
        }
        if (original.getStatus() != Bid.BidStatus.COUNTERED) {
            throw new RuntimeException("Only a countered bid can be revised");
        }

        Bid revised = Bid.builder()
                .taskId(original.getTaskId())
                .workerId(workerId)
                .amount(request.getAmount())
                .message(request.getMessage())
                .estimatedDurationHours(request.getEstimatedDurationHours())
                .status(Bid.BidStatus.PENDING)
                .parentBidId(original.getParentBidId() != null ? original.getParentBidId() : original.getId())
                .expiresAt(original.getExpiresAt())
                .build();
        revised = bidRepository.save(revised);

        original.setStatus(Bid.BidStatus.REVISED);
        bidRepository.save(original);

        User worker = userRepository.findById(workerId).orElse(null);
        return bidMapper.toResponse(revised, worker);
    }

    @Override
    public List<BidDtos.BidResponse> getBidHistory(Long bidId) {
        Bid root = bidRepository.findById(bidId)
                .orElseThrow(() -> new RuntimeException("Bid not found"));
        Long rootId = root.getParentBidId() != null ? root.getParentBidId() : root.getId();
        List<Bid> chain = bidRepository.findByParentBidId(rootId);
        chain.add(root);
        return chain.stream()
                .map(bid -> {
                    User worker = userRepository.findById(bid.getWorkerId()).orElse(null);
                    return bidMapper.toResponse(bid, worker);
                })
                .collect(Collectors.toList());
    }
}