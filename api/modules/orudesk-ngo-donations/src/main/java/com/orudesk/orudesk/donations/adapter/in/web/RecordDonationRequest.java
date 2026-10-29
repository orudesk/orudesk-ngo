package com.orudesk.orudesk.donations.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.UUID;

/**
 * The JSON body of POST /api/v1/donations.
 *
 * @param amount a string such as "1500" or "1500.50": a JSON number would be read as floating point by most
 *               clients (JavaScript has no other kind), and money must stay exact
 */
record RecordDonationRequest(
        @NotNull UUID donorId,
        @NotBlank @Pattern(regexp = "^[0-9]{1,12}(\\.[0-9]{1,2})?$", message = "must be rupees, like 1500 or 1500.50")
        @Schema(example = "1500.00") String amount,
        @NotNull LocalDate receivedOn,
        @NotBlank @Schema(allowableValues = {"CASH", "CHEQUE", "BANK_TRANSFER", "UPI", "CARD", "ONLINE"}) String mode,
        @Size(max = 50) @Nullable String reference,
        @Size(max = 100) @Nullable String purpose) {
}
