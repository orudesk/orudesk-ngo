package com.orudesk.orudesk.donors.adapter.in.web;

import com.orudesk.orudesk.donors.application.port.in.DonorDetails;
import com.orudesk.orudesk.donors.application.port.in.GetDonorQuery;
import com.orudesk.orudesk.donors.application.port.in.RegisterDonorCommand;
import com.orudesk.orudesk.donors.application.port.in.RegisterDonorUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/donors")
@Tag(name = "Donors")
@PreAuthorize("hasAnyRole('ADMIN', 'DONOR_MANAGER')")
class DonorController {

    private final RegisterDonorUseCase registerDonor;
    private final GetDonorQuery getDonor;

    DonorController(RegisterDonorUseCase registerDonor, GetDonorQuery getDonor) {
        this.registerDonor = registerDonor;
        this.getDonor = getDonor;
    }

    @PostMapping
    @Operation(operationId = "registerDonor", summary = "Add a donor (and the person, to the registry)")
    ResponseEntity<RegisterDonorResponse> register(@Valid @RequestBody RegisterDonorRequest request) {
        UUID id = registerDonor.register(new RegisterDonorCommand(request.firstName(), request.lastName(),
                request.dateOfBirth(), request.phone(), request.email(), request.pan(), Boolean.TRUE.equals(request.receivesUpdates())));
        return ResponseEntity.created(URI.create("/api/v1/donors/" + id)).body(new RegisterDonorResponse(id));
    }

    @GetMapping("/{donorId}")
    @Operation(operationId = "getDonor", summary = "One donor, with their details from the registry")
    DonorDetails get(@PathVariable UUID donorId) {
        return getDonor.getDonor(donorId);
    }
}
