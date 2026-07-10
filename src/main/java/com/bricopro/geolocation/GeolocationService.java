package com.bricopro.geolocation;

import com.bricopro.user.entity.WorkerProfile.ServiceType;
import com.bricopro.user.repository.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GeolocationService {

    private final WorkerProfileRepository workerProfileRepository;
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Find nearby workers (paginated, with optional service filter)
     */
    public Page<NearbyWorkerDto> findNearbyWorkers(double lat, double lng,
                                                   double radiusKm,
                                                   ServiceType serviceType,
                                                   Pageable pageable) {
        return workerProfileRepository
                .findNearbyWorkers(lat, lng, radiusKm, serviceType, pageable)
                .map(p -> {
                    double dist = haversine(lat, lng, p.getLatitude(), p.getLongitude());
                    return NearbyWorkerDto.builder()
                            .userId(p.getUser().getId())
                            .firstName(p.getUser().getFirstName())
                            .lastName(p.getUser().getLastName())
                            .avatarUrl(p.getUser().getAvatarUrl())
                            .averageRating(p.getAverageRating().doubleValue())
                            .totalReviews(p.getTotalReviews())
                            .distanceKm(Math.round(dist * 10.0) / 10.0)
                            .city(p.getCity())
                            .isPremium(p.isPremium())
                            .cinVerified(p.isCinVerified())
                            .build();
                });
    }

    /**
     * Simple method for frontend – returns a list (limit 20) without service filter
     */
    public List<NearbyWorkerDto> findNearbyWorkers(double lat, double lng, double radiusKm) {
        Page<NearbyWorkerDto> page = findNearbyWorkers(lat, lng, radiusKm, null, PageRequest.of(0, 20));
        return page.getContent();
    }

    /**
     * Haversine distance calculation (kept static for reuse)
     */
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}