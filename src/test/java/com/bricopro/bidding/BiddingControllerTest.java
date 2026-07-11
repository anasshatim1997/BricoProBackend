package com.bricopro.bidding;

import com.bricopro.bidding.controller.BiddingController;
import com.bricopro.bidding.dto.BidDtos;
import com.bricopro.bidding.service.IBiddingService;
import com.bricopro.security.jwt.JwtService;
import com.bricopro.security.jwt.TokenBlacklistService;
import com.bricopro.security.oauth2.OAuth2SuccessHandler;
import com.bricopro.user.entity.User;
import com.bricopro.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BiddingController.class)
@AutoConfigureMockMvc(addFilters = false)
class BiddingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IBiddingService biddingService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void asWorker(long id) {
        User worker = User.builder()
                .id(id)
                .email("worker" + id + "@bricopro.ma")
                .role(User.Role.WORKER)
                .status(User.Status.ACTIVE)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(worker, null, worker.getAuthorities()));
    }

    private void asClient(long id) {
        User client = User.builder()
                .id(id)
                .email("client" + id + "@bricopro.ma")
                .role(User.Role.CLIENT)
                .status(User.Status.ACTIVE)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(client, null, client.getAuthorities()));
    }

    @Test
    void createBid_success() throws Exception {
        asWorker(1L);

        BidDtos.CreateBidRequest request = new BidDtos.CreateBidRequest();
        request.setTaskId(1L);
        request.setAmount(BigDecimal.valueOf(100));

        BidDtos.BidResponse response = new BidDtos.BidResponse();
        response.setId(1L);
        response.setTaskId(1L);
        response.setWorkerId(1L);
        response.setAmount(BigDecimal.valueOf(100));
        response.setStatus("PENDING");

        when(biddingService.createBid(eq(1L), any(BidDtos.CreateBidRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.workerId").value(1));
    }

    @Test
    void acceptBid_success() throws Exception {
        asClient(1L);

        doNothing().when(biddingService).acceptBid(1L, 1L);

        mockMvc.perform(post("/api/v1/bids/1/accept"))
                .andExpect(status().isOk());

        verify(biddingService).acceptBid(1L, 1L);
    }

    @Test
    void rejectBid_success() throws Exception {
        asClient(1L);

        doNothing().when(biddingService).rejectBid(1L, 1L);

        mockMvc.perform(post("/api/v1/bids/1/reject"))
                .andExpect(status().isOk());

        verify(biddingService).rejectBid(1L, 1L);
    }

    @Test
    void withdrawBid_success() throws Exception {
        asWorker(1L);

        doNothing().when(biddingService).withdrawBid(1L, 1L);

        mockMvc.perform(delete("/api/v1/bids/1"))
                .andExpect(status().isNoContent());

        verify(biddingService).withdrawBid(1L, 1L);
    }

    @Test
    void getBidsForTask_success() throws Exception {
        asClient(1L);

        when(biddingService.getBidsForTask(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/bids/task/1"))
                .andExpect(status().isOk());

        verify(biddingService).getBidsForTask(1L);
    }

    @Test
    void getMyBids_success() throws Exception {
        asWorker(1L);

        when(biddingService.getBidsByWorker(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/bids/worker"))
                .andExpect(status().isOk());

        verify(biddingService).getBidsByWorker(1L);
    }
}