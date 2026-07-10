package com.bricopro.bidding;

import com.bricopro.bidding.dto.BidDtos;
import com.bricopro.bidding.entity.Bid;
import com.bricopro.bidding.mapper.BidMapper;
import com.bricopro.bidding.repository.BidRepository;
import com.bricopro.bidding.service.BiddingServiceImpl;
import com.bricopro.task.entity.Task;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BiddingServiceTest {

    @Mock
    private BidRepository bidRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BidMapper bidMapper;

    @InjectMocks
    private BiddingServiceImpl biddingService;

    private Task task;
    private User worker;
    private Bid bid;
    private BidDtos.CreateBidRequest createRequest;
    private BidDtos.UpdateBidRequest updateRequest;

    @BeforeEach
    void setUp() {
        worker = User.builder().id(1L).firstName("John").lastName("Doe").build();
        User client = User.builder().id(2L).firstName("Jane").lastName("Smith").build();

        task = Task.builder()
                .id(1L)
                .client(client)
                .biddingEnabled(true)
                .biddingDeadline(LocalDateTime.now().plusDays(1))
                .build();

        bid = Bid.builder()
                .id(1L)
                .taskId(1L)
                .workerId(1L)
                .amount(BigDecimal.valueOf(100))
                .status(Bid.BidStatus.PENDING)
                .build();

        createRequest = new BidDtos.CreateBidRequest();
        createRequest.setTaskId(1L);
        createRequest.setAmount(BigDecimal.valueOf(100));
        createRequest.setMessage("I can do it");

        updateRequest = new BidDtos.UpdateBidRequest();
        updateRequest.setAmount(BigDecimal.valueOf(120));
        updateRequest.setMessage("Updated");
    }

    @Test
    void createBid_success() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(bidRepository.findByTaskIdAndWorkerId(1L, 1L)).thenReturn(Optional.empty());
        when(bidMapper.toEntity(createRequest, 1L)).thenReturn(bid);
        when(bidRepository.save(any(Bid.class))).thenReturn(bid);
        when(userRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(bidMapper.toResponse(any(Bid.class), any(User.class))).thenReturn(new BidDtos.BidResponse());

        BidDtos.BidResponse response = biddingService.createBid(1L, createRequest);

        assertThat(response).isNotNull();
        verify(bidRepository).save(any(Bid.class));
    }

    @Test
    void createBid_biddingDisabled_throwsException() {
        task.setBiddingEnabled(false);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> biddingService.createBid(1L, createRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Bidding is not enabled for this task");
    }

    @Test
    void createBid_deadlinePassed_throwsException() {
        task.setBiddingDeadline(LocalDateTime.now().minusMinutes(1));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> biddingService.createBid(1L, createRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Bidding deadline has passed");
    }

    @Test
    void createBid_alreadyBid_throwsException() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(bidRepository.findByTaskIdAndWorkerId(1L, 1L)).thenReturn(Optional.of(bid));

        assertThatThrownBy(() -> biddingService.createBid(1L, createRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("You have already placed a bid on this task");
    }

    @Test
    void acceptBid_success() {
        when(bidRepository.findById(1L)).thenReturn(Optional.of(bid));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1L)).thenReturn(Optional.of(worker));

        biddingService.acceptBid(1L, 2L);

        assertThat(bid.getStatus()).isEqualTo(Bid.BidStatus.ACCEPTED);
        assertThat(task.getWorker()).isEqualTo(worker);
        assertThat(task.getStatus()).isEqualTo(Task.TaskStatus.CONFIRMED);
        verify(bidRepository).save(bid);
        verify(taskRepository).save(task);
        verify(bidRepository).updateStatusIfPending(1L, Bid.BidStatus.REJECTED);
    }

    @Test
    void acceptBid_notClient_throwsException() {
        when(bidRepository.findById(1L)).thenReturn(Optional.of(bid));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> biddingService.acceptBid(1L, 3L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Only the task client can accept bids");
    }

    @Test
    void rejectBid_success() {
        when(bidRepository.findById(1L)).thenReturn(Optional.of(bid));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        biddingService.rejectBid(1L, 2L);

        assertThat(bid.getStatus()).isEqualTo(Bid.BidStatus.REJECTED);
        verify(bidRepository).save(bid);
    }

    @Test
    void updateBid_success() {
        when(bidRepository.findById(1L)).thenReturn(Optional.of(bid));
        when(userRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(bidRepository.save(any(Bid.class))).thenReturn(bid);
        when(bidMapper.toResponse(any(Bid.class), any(User.class))).thenReturn(new BidDtos.BidResponse());

        BidDtos.BidResponse response = biddingService.updateBid(1L, 1L, updateRequest);

        assertThat(response).isNotNull();
        assertThat(bid.getAmount()).isEqualTo(BigDecimal.valueOf(120));
        assertThat(bid.getMessage()).isEqualTo("Updated");
        verify(bidRepository).save(bid);
    }

    @Test
    void updateBid_notOwner_throwsException() {
        when(bidRepository.findById(1L)).thenReturn(Optional.of(bid));

        assertThatThrownBy(() -> biddingService.updateBid(1L, 2L, updateRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Not authorized to update this bid");
    }

    @Test
    void withdrawBid_success() {
        when(bidRepository.findById(1L)).thenReturn(Optional.of(bid));

        biddingService.withdrawBid(1L, 1L);

        assertThat(bid.getStatus()).isEqualTo(Bid.BidStatus.WITHDRAWN);
        verify(bidRepository).save(bid);
    }

    @Test
    void expirePendingBids() {
        biddingService.expirePendingBids();
        verify(bidRepository).expirePendingBids();
    }
}