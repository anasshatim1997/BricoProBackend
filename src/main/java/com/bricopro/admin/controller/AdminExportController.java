package com.bricopro.admin.controller;

import com.bricopro.admin.service.AdminExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/v1/admin/export")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Export")
@SecurityRequirement(name = "bearerAuth")
public class AdminExportController {

    private final AdminExportService exportService;

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/users/csv")
    @Operation(
        summary = "Export all users as CSV",
        description = "Export Users Csv. Returns a paginated list of matching resources. Use the query parameters to filter and control sort order."
    )
    public ResponseEntity<byte[]> exportUsersCsv() {
        byte[] csv = exportService.exportUsersCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bricopro_users.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/tasks/csv")
    @Operation(
        summary = "Export tasks as CSV — optional date range filter",
        description = "Export Tasks Csv. Returns a paginated list of matching resources. Use the query parameters to filter and control sort order."
    )
    public ResponseEntity<byte[]> exportTasksCsv(
            @Parameter(name = "from", description = "Query filter or pagination parameter", required = false, in = ParameterIn.QUERY) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(name = "param", description = "Query filter or pagination parameter", required = false, in = ParameterIn.QUERY) @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] csv = exportService.exportTasksCsv(from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bricopro_tasks.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/revenue/csv")
    @Operation(
        summary = "Export platform revenue as CSV by month",
        description = "Export Revenue Csv. Returns a paginated list of matching resources. Use the query parameters to filter and control sort order."
    )
    public ResponseEntity<byte[]> exportRevenueCsv(@Parameter(name = "param", description = "Query filter or pagination parameter", required = true, in = ParameterIn.QUERY) @RequestParam int year) {
        byte[] csv = exportService.exportRevenueCsv(year);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"bricopro_revenue_" + year + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/revenue/pdf")
    @Operation(
        summary = "Export platform revenue report as PDF",
        description = "Export Revenue Pdf. Returns a paginated list of matching resources. Use the query parameters to filter and control sort order."
    )
    public ResponseEntity<byte[]> exportRevenuePdf(@Parameter(name = "param", description = "Query filter or pagination parameter", required = true, in = ParameterIn.QUERY) @RequestParam int year) {
        byte[] pdf = exportService.exportRevenuePdf(year);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"bricopro_revenue_" + year + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/workers/csv")
    @Operation(
        summary = "Export worker performance report as CSV",
        description = "Export Workers Csv. Returns a paginated list of matching resources. Use the query parameters to filter and control sort order."
    )
    public ResponseEntity<byte[]> exportWorkersCsv() {
        byte[] csv = exportService.exportWorkerPerformanceCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bricopro_workers.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
