package com.bricopro.geolocation;

import com.bricopro.user.entity.WorkerProfile.ServiceType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.List;

@RestController
@RequestMapping("/api/v1/geolocation")
@RequiredArgsConstructor
@Tag(name = "Geolocation")
@SecurityRequirement(name = "bearerAuth")
public class GeolocationController {

    private final GeolocationService geolocationService;

    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping
    @Operation(
            summary = "Find workers within radius using GPS coordinates (Haversine)",
            description = "Find Nearby. Returns a paginated list of matching resources. Use the query parameters to filter and control sort order."
    )
    public ResponseEntity<Page<NearbyWorkerDto>> findNearby(
            @Parameter(name = "lat", description = "Query filter or pagination parameter", required = true, in = ParameterIn.QUERY) @RequestParam double lat,
            @Parameter(name = "lng", description = "Query filter or pagination parameter", required = true, in = ParameterIn.QUERY) @RequestParam double lng,
            @Parameter(name = "radiusKm", description = "Query filter or pagination parameter", required = true, in = ParameterIn.QUERY) @RequestParam(defaultValue = "20") double radiusKm,
            @Parameter(name = "serviceType", description = "Query filter or pagination parameter", required = false, in = ParameterIn.QUERY) @RequestParam(required = false) ServiceType serviceType,
            Pageable pageable) {
        return ResponseEntity.ok(
                geolocationService.findNearbyWorkers(lat, lng, radiusKm, serviceType, pageable));
    }

    @GetMapping("/nearby")
    @Operation(
            summary = "Simplified nearby-workers lookup (non-paginated, top 20, no service filter)",
            description = "Lightweight variant of the default GET endpoint above, kept for clients that only need " +
                    "a quick top-N list without pagination or service-type filtering. " +
                    "For anything requiring filtering or paging, use GET /api/v1/geolocation instead."
    )
    public List<NearbyWorkerDto> getNearbyWorkers(
            @Parameter(name = "lat", description = "Latitude of the search origin", required = true, in = ParameterIn.QUERY) @RequestParam double lat,
            @Parameter(name = "lng", description = "Longitude of the search origin", required = true, in = ParameterIn.QUERY) @RequestParam double lng,
            @Parameter(name = "radius", description = "Search radius in kilometers", required = false, in = ParameterIn.QUERY) @RequestParam(defaultValue = "10") double radius) {
        return geolocationService.findNearbyWorkers(lat, lng, radius);
    }
}