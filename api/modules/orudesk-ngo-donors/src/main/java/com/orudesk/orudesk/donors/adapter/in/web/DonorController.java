package com.orudesk.orudesk.donors.adapter.in.web;

import com.orudesk.orudesk.donors.application.port.in.CountDonorsQuery;
import com.orudesk.orudesk.donors.application.port.in.DonorCount;
import com.orudesk.orudesk.donors.application.port.in.DonorDetails;
import com.orudesk.orudesk.donors.application.port.in.DonorPage;
import com.orudesk.orudesk.donors.application.port.in.DonorSearch;
import com.orudesk.orudesk.donors.application.port.in.GetDonorQuery;
import com.orudesk.orudesk.donors.application.port.in.RegisterDonorCommand;
import com.orudesk.orudesk.donors.application.port.in.RegisterDonorUseCase;
import com.orudesk.orudesk.donors.application.port.in.SearchDonorsQuery;
import com.orudesk.orudesk.donors.domain.DonorType;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/donors")
@Tag(name = "Donors")
@PreAuthorize("hasAnyRole('ADMIN', 'DONOR_MANAGER')")
class DonorController {

    private final RegisterDonorUseCase registerDonor;
    private final GetDonorQuery getDonor;
    private final SearchDonorsQuery searchDonors;
    private final CountDonorsQuery countDonors;

    DonorController(RegisterDonorUseCase registerDonor, GetDonorQuery getDonor, SearchDonorsQuery searchDonors,
                    CountDonorsQuery countDonors) {
        this.registerDonor = registerDonor;
        this.getDonor = getDonor;
        this.searchDonors = searchDonors;
        this.countDonors = countDonors;
    }

    @PostMapping
    @Operation(operationId = "registerDonor", summary = "Add a donor (and the person, to the registry)")
    @ApiResponse(responseCode = "201", description = "Created", useReturnTypeSchema = true)
    ResponseEntity<RegisterDonorResponse> register(@Valid @RequestBody RegisterDonorRequest request) {
        UUID id = registerDonor.register(new RegisterDonorCommand(request.firstName(), request.lastName(),
                request.dateOfBirth(), request.phone(), request.email(), request.type(), request.pan(),
                Boolean.TRUE.equals(request.wantsTaxReceipt()), Boolean.TRUE.equals(request.receivesUpdates())));
        return ResponseEntity.created(URI.create("/api/v1/donors/" + id)).body(new RegisterDonorResponse(id));
    }

    /** GET /api/v1/donors?q=meera&type=INDIVIDUAL&size=20&after=... */
    @GetMapping
    @Operation(operationId = "searchDonors", summary = "Find donors by part of a name, phone or email, one page at a time")
    DonorPage search(@RequestParam(required = false) String q,
                     @RequestParam(required = false) DonorType type,
                     @RequestParam(required = false) String after,
                     @RequestParam(defaultValue = "20") int size) {
        return searchDonors.search(new DonorSearch(q, type, after, size));
    }

    @GetMapping("/counts")
    @Operation(operationId = "countDonors", summary = "How many donors of each type, and how many want tax receipts")
    List<DonorCount> counts() {
        return countDonors.countByType();
    }

    @GetMapping("/{donorId}")
    @Operation(operationId = "getDonor", summary = "One donor, with their details from the registry")
    DonorDetails get(@PathVariable UUID donorId) {
        return getDonor.getDonor(donorId);
    }
}
