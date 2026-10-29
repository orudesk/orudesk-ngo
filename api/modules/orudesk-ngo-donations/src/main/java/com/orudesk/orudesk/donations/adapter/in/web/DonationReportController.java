package com.orudesk.orudesk.donations.adapter.in.web;

import com.orudesk.orudesk.donations.application.port.in.DonationReportsQuery;
import com.orudesk.orudesk.donations.application.port.in.DonationSummary;
import com.orudesk.orudesk.donations.application.port.in.Form10BDRow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@Tag(name = "Donations")
@PreAuthorize("hasAnyRole('ADMIN', 'DONOR_MANAGER')")
class DonationReportController {

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final DonationReportsQuery reports;

    DonationReportController(DonationReportsQuery reports) {
        this.reports = reports;
    }

    /** GET /api/v1/donations/summary?financialYear=2026-27 */
    @GetMapping("/api/v1/donations/summary")
    @Operation(operationId = "summariseDonations", summary = "A financial year's donations by month and by payment mode")
    DonationSummary summary(@RequestParam String financialYear) {
        return reports.summary(financialYear);
    }

    /**
     * The CSV for Form 10BD (Day 59), to check and upload on the income tax portal. CSV is a presentation of the
     * rows, so it's written here, in the web adapter, not in the use case.
     */
    @GetMapping(value = "/api/v1/donations/form-10bd", produces = "text/csv")
    @Operation(operationId = "exportForm10BD", summary = "The year's 80G-deductible donations as CSV, for Form 10BD")
    @ApiResponse(responseCode = "200", description = "The CSV file",
            content = @Content(mediaType = "text/csv", schema = @Schema(type = "string")))
    ResponseEntity<String> form10BD(@RequestParam String financialYear) {
        List<Form10BDRow> rows = reports.form10BD(financialYear);
        StringBuilder csv = new StringBuilder("Sr. No.,ID Code (PAN),Name of donor,Donation type,Mode of receipt,Amount of donation (INR)\r\n");
        for (Form10BDRow row : rows) {
            csv.append(row.serialNumber()).append(',')
                    .append(Csv.field(row.pan())).append(',')
                    .append(Csv.field(row.donorName())).append(',')
                    .append(Csv.field(row.donationType())).append(',')
                    .append(Csv.field(row.modeOfReceipt())).append(',')
                    .append(row.amount()).append("\r\n");
        }
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("form-10bd-" + financialYear + ".csv").build().toString())
                .body(csv.toString());
    }
}
