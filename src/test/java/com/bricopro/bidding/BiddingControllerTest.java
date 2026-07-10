package com.bricopro.bidding;

import com.bricopro.bidding.controller.BiddingController;
import com.bricopro.bidding.dto.BidDtos;
import com.bricopro.bidding.service.BiddingServiceImpl;
import com.bricopro.config.SecurityConfig;
import com.bricopro.security.filter.JwtAuthFilter;
import com.bricopro.security.oauth2.OAuth2SuccessHandler;
import com.bricopro.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BiddingController.class)
@Import(SecurityConfig.class)
class BiddingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BiddingServiceImpl biddingService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    private RequestPostProcessor asWorker(long id) {
        User worker = User.builder()
                .id(id)
                .email("worker" + id + "@bricopro.ma")
                .role(User.Role.WORKER)
                .status(User.Status.ACTIVE)
                .build();
        return SecurityMockMvcRequestPostProcessors.user(worker);
    }

    private RequestPostProcessor asClient(long id) {
        User client = User.builder()
                .id(id)
                .email("client" + id + "@bricopro.ma")
                .role(User.Role.CLIENT)
                .status(User.Status.ACTIVE)
                .build();
        return SecurityMockMvcRequestPostProcessors.user(client);
    }

    @Test
    void createBid_success() throws Exception {
        BidDtos.CreateBidRequest request = new BidDtos.CreateBidRequest();
        request.setTaskId(1L);
        request.setAmount(BigDecimal.valueOf(100));

        BidDtos.BidResponse response = new BidDtos.BidResponse();
        response.setId(1L);
        response.setTaskId(1L);
        response.setWorkerId(1L);
        response.setAmount(BigDecimal.valueOf(100));
        response.setStatus("PENDING");

        when(biddingService.createBid(eq(1L), any(BidDtos.CreateBidRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/bids")
                        .with(asWorker(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.workerId").value(1));
    }

    @Test
    void acceptBid_success() throws Exception {
        doNothing().when(biddingService).acceptBid(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/bids/1/accept").with(asClient(1L)))
                .andExpect(status().isOk());

        verify(biddingService).acceptBid(1L, 1L);
    }

    @Test
    void rejectBid_success() throws Exception {
        doNothing().when(biddingService).rejectBid(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/bids/1/reject").with(asClient(1L)))
                .andExpect(status().isOk());

        verify(biddingService).rejectBid(1L, 1L);
    }

    @Test
    void withdrawBid_success() throws Exception {
        doNothing().when(biddingService).withdrawBid(eq(1L), eq(1L));

        mockMvc.perform(delete("/api/v1/bids/1").with(asWorker(1L)))
                .andExpect(status().isNoContent());

        verify(biddingService).withdrawBid(1L, 1L);
    }

    @Test
    void getBidsForTask_success() throws Exception {
        mockMvc.perform(get("/api/v1/bids/task/1").with(asClient(1L)))
                .andExpect(status().isOk());

        verify(biddingService).getBidsForTask(1L);
    }

    @Test
    void getMyBids_success() throws Exception {
        mockMvc.perform(get("/api/v1/bids/worker").with(asWorker(1L)))
                .andExpect(status().isOk());

        verify(biddingService).getBidsByWorker(1L);
    }
}
