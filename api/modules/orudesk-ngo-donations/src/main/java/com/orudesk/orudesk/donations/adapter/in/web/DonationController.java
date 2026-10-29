package com.orudesk.orudesk.donations.adapter.in.web;

import com.orudesk.orudesk.donations.application.port.in.CancelDonationCommand;
import com.orudesk.orudesk.donations.application.port.in.CancelDonationUseCase;
import com.orudesk.orudesk.donations.application.port.in.DonationDetails;
import com.orudesk.orudesk.donations.application.port.in.DonationReceipt;
import com.orudesk.orudesk.donations.application.port.in.DonorDonations;
import com.orudesk.orudesk.donations.application.port.in.GetDonationQuery;
import com.orudesk.orudesk.donations.application.port.in.RecordDonationCommand;
import com.orudesk.orudesk.donations.application.port.in.RecordDonationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/** The people who manage donors record what they give (the DONOR_MANAGER role comes from the donors module). */
@RestController
@Tag(name = "Donations")
@PreAuthorize("hasAnyRole('ADMIN', 'DONOR_MANAGER')")
class DonationController {

    private final RecordDonationUseCase recordDonation;
    private final CancelDonationUseCase cancelDonation;
    private final GetDonationQuery getDonation;

    DonationController(RecordDonationUseCase recordDonation, CancelDonationUseCase cancelDonation,
                       GetDonationQuery getDonation) {
        this.recordDonation = recordDonation;
        this.cancelDonation = cancelDonation;
        this.getDonation = getDonation;
    }

    @PostMapping("/api/v1/donations")
    @Operation(operationId = "recordDonation", summary = "Record money a donor gave")
    @ApiResponse(responseCode = "201", description = "Created", useReturnTypeSchema = true)
    ResponseEntity<RecordDonationResponse> record(@Valid @RequestBody RecordDonationRequest request) {
        UUID id = recordDonation.record(new RecordDonationCommand(request.donorId(), request.amount(),
                request.receivedOn(), request.mode(), request.reference(), request.purpose()));
        return ResponseEntity.created(URI.create("/api/v1/donations/" + id)).body(new RecordDonationResponse(id));
    }

    @GetMapping("/api/v1/donations/{donationId}")
    @Operation(operationId = "getDonation", summary = "One donation")
    DonationDetails get(@PathVariable UUID donationId) {
        return getDonation.getDonation(donationId);
    }

    /** An action, not a DELETE: the donation stays, marked cancelled, with its receipt number (Day 58). */
    @PostMapping("/api/v1/donations/{donationId}/cancel")
    @Operation(operationId = "cancelDonation", summary = "Cancel a donation (a bounced cheque, a duplicate entry)",
            description = "Send the version you read. 409 if the donation changed since, or is already cancelled.")
    DonationDetails cancel(@PathVariable UUID donationId, @Valid @RequestBody CancelDonationRequest request) {
        return cancelDonation.cancel(new CancelDonationCommand(donationId, request.version(), request.reason()));
    }

    @GetMapping("/api/v1/donations/{donationId}/receipt")
    @Operation(operationId = "getDonationReceipt", summary = "A donation's receipt, with the amount in words and whether it's 80G-deductible")
    DonationReceipt receipt(@PathVariable UUID donationId) {
        return getDonation.receiptFor(donationId);
    }

    @GetMapping("/api/v1/donors/{donorId}/donations")
    @Operation(operationId = "listDonorDonations", summary = "Everything a donor gave, newest first, and the total")
    DonorDonations ofDonor(@PathVariable UUID donorId) {
        return getDonation.donationsOf(donorId);
    }
}
