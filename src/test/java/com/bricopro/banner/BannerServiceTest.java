package com.bricopro.banner;

import com.bricopro.banner.entity.Banner;
import com.bricopro.banner.repository.BannerRepository;
import com.bricopro.banner.service.BannerService;
import com.bricopro.home.dto.BannerDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BannerService")
class BannerServiceTest {

    @Mock BannerRepository bannerRepository;

    @InjectMocks BannerService bannerService;

    @Test
    @DisplayName("maps active banners to DTOs")
    void mapsActiveBannersToDto() {
        Banner banner = Banner.builder()
                .id(1L).title("Promo été").description("-20% sur le nettoyage")
                .imageUrl("http://cdn/banner1.jpg").linkUrl("bricopro://promo/1")
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .build();

        when(bannerRepository.findActiveBanners(any())).thenReturn(List.of(banner));

        List<BannerDto> result = bannerService.getActiveBanners();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getTitle()).isEqualTo("Promo été");
        assertThat(result.get(0).getImageUrl()).isEqualTo("http://cdn/banner1.jpg");
    }

    @Test
    @DisplayName("returns an empty list when no banners are currently active")
    void returnsEmptyWhenNoneActive() {
        when(bannerRepository.findActiveBanners(any())).thenReturn(List.of());

        assertThat(bannerService.getActiveBanners()).isEmpty();
    }

    @Test
    @DisplayName("passes the current time to the repository query")
    void passesCurrentTime() {
        when(bannerRepository.findActiveBanners(any())).thenReturn(List.of());

        LocalDateTime before = LocalDateTime.now();
        bannerService.getActiveBanners();
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(bannerRepository).findActiveBanners(captor.capture());
        assertThat(captor.getValue()).isBetween(before, after);
    }
}
