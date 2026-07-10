package com.bricopro.service;

import com.bricopro.home.dto.ServiceDto;
import com.bricopro.service.entity.ServiceCategory;
import com.bricopro.service.repository.ServiceCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceCategoryService")
class ServiceCategoryServiceTest {

    @Mock ServiceCategoryRepository repository;

    @InjectMocks ServiceCategoryService service;

    private ServiceCategory plumbing;

    @BeforeEach
    void setup() {
        plumbing = ServiceCategory.builder()
                .id(1L).key("PLUMBING").frName("Plomberie").arName("سباكة")
                .icon("🚿").color("#3B82F6").priceMin(100).priceMax(500)
                .displayOrder(9).active(true)
                .build();
    }

    @Nested
    @DisplayName("getAllActive()")
    class GetAllActive {

        @Test
        @DisplayName("maps entities to DTOs preserving order")
        void mapsToDto() {
            when(repository.findByActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(plumbing));

            List<ServiceDto> result = service.getAllActive();

            assertThat(result).hasSize(1);
            ServiceDto dto = result.get(0);
            assertThat(dto.getKey()).isEqualTo("PLUMBING");
            assertThat(dto.getFr()).isEqualTo("Plomberie");
            assertThat(dto.getAr()).isEqualTo("سباكة");
            assertThat(dto.getIcon()).isEqualTo("🚿");
            assertThat(dto.getColor()).isEqualTo("#3B82F6");
            assertThat(dto.getPriceMin()).isEqualTo(100);
            assertThat(dto.getPriceMax()).isEqualTo(500);
        }

        @Test
        @DisplayName("returns an empty list when no categories are active")
        void returnsEmptyWhenNoneActive() {
            when(repository.findByActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of());

            assertThat(service.getAllActive()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByKey()")
    class FindByKey {

        @Test
        @DisplayName("returns the mapped DTO when found")
        void returnsDto() {
            when(repository.findByKey("PLUMBING")).thenReturn(Optional.of(plumbing));

            ServiceDto result = service.findByKey("PLUMBING");

            assertThat(result).isNotNull();
            assertThat(result.getKey()).isEqualTo("PLUMBING");
        }

        @Test
        @DisplayName("returns null when the key doesn't exist")
        void returnsNullWhenNotFound() {
            when(repository.findByKey("NONEXISTENT")).thenReturn(Optional.empty());

            assertThat(service.findByKey("NONEXISTENT")).isNull();
        }
    }

    @Nested
    @DisplayName("searchByName()")
    class SearchByName {

        @Test
        @DisplayName("searches by French or Arabic name")
        void searchesBothLanguages() {
            when(repository.findByFrNameContainingIgnoreCaseOrArNameContainingIgnoreCase("plomb", "plomb"))
                    .thenReturn(List.of(plumbing));

            List<ServiceDto> result = service.searchByName("plomb");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getKey()).isEqualTo("PLUMBING");
        }

        @Test
        @DisplayName("trims whitespace before searching")
        void trimsQuery() {
            when(repository.findByFrNameContainingIgnoreCaseOrArNameContainingIgnoreCase("plomb", "plomb"))
                    .thenReturn(List.of());

            service.searchByName("  plomb  ");

            verify(repository).findByFrNameContainingIgnoreCaseOrArNameContainingIgnoreCase("plomb", "plomb");
        }

        @Test
        @DisplayName("returns empty for null query without touching the repository")
        void nullQueryReturnsEmpty() {
            assertThat(service.searchByName(null)).isEmpty();
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("returns empty for a single-character query without touching the repository")
        void tooShortQueryReturnsEmpty() {
            assertThat(service.searchByName("p")).isEmpty();
            verifyNoInteractions(repository);
        }
    }
}
