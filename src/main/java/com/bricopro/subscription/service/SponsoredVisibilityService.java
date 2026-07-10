package com.bricopro.subscription.service;

import com.bricopro.subscription.dto.CreateCampaignRequest;
import com.bricopro.subscription.entity.SponsoredClick;
import com.bricopro.subscription.entity.SponsoredWorker;
import com.bricopro.subscription.repository.SponsoredClickRepository;
import com.bricopro.subscription.repository.SponsoredWorkerRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import com.bricopro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SponsoredVisibilityService {

    private final SponsoredWorkerRepository sponsoredRepo;
    private final SponsoredClickRepository  clickRepo;
    private final UserRepository            userRepository;

    @Transactional
    public SponsoredWorker createCampaign(Long workerId, CreateCampaignRequest req) {
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found"));

        return sponsoredRepo.save(SponsoredWorker.builder()
                .worker(worker)
                .serviceType(req.getServiceType())
                .targetCity(req.getTargetCity())
                .dailyBudget(req.getDailyBudget())
                .costPerClick(req.getCostPerClick())
                .spent(BigDecimal.ZERO)
                .impressions(0)
                .clicks(0)
                .startsAt(LocalDateTime.now())
                .endsAt(LocalDateTime.now().plusDays(req.getDurationDays()))
                .active(true)
                .build());
    }

    public List<SponsoredWorker> getSponsored(ServiceType serviceType, String city) {
        List<SponsoredWorker> sponsored = sponsoredRepo.findActiveSponsoredWorkers(serviceType, city);
        sponsored.forEach(s -> {
            s.setImpressions(s.getImpressions() + 1);
            sponsoredRepo.save(s);
        });
        return sponsored;
    }

    @Transactional
    public boolean recordClick(Long sponsoredId, Long viewerId) {
        if (clickRepo.existsBySponsoredWorkerIdAndViewerId(sponsoredId, viewerId)) {
            return false;
        }

        try {
            clickRepo.save(SponsoredClick.builder()
                    .sponsoredWorkerId(sponsoredId)
                    .viewerId(viewerId)
                    .build());
        } catch (DataIntegrityViolationException e) {
            return false;
        }

        return sponsoredRepo.findById(sponsoredId).map(s -> {
            s.setClicks(s.getClicks() + 1);
            s.setSpent(s.getSpent().add(s.getCostPerClick()));
            if (s.getSpent().compareTo(s.getDailyBudget()) >= 0) {
                s.setActive(false);
            }
            sponsoredRepo.save(s);
            return true;
        }).orElse(false);
    }

    public Map<String, Object> getCampaignStats(Long workerId) {
        List<SponsoredWorker> myCampaigns = sponsoredRepo.findByWorkerId(workerId);

        return Map.of(
                "activeCampaigns", myCampaigns.stream().filter(SponsoredWorker::isActive).count(),
                "totalImpressions", myCampaigns.stream().mapToLong(SponsoredWorker::getImpressions).sum(),
                "totalClicks", myCampaigns.stream().mapToLong(SponsoredWorker::getClicks).sum()
        );
    }
}
