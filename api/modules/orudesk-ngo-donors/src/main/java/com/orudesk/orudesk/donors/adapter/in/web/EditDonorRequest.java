package com.orudesk.orudesk.donors.adapter.in.web;

import com.orudesk.orudesk.donors.domain.DonorType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

/**
 * The JSON body of PUT /api/v1/donors/{id}: the whole form, and the two versions it was read at.
 *
 * @param type left out keeps the type as it is
 * @param pan  left out keeps the PAN, "" removes it, a value replaces it (the form only ever saw it masked)
 */
record EditDonorRequest(
        @NotNull @PositiveOrZero Long version,
        @NotNull @PositiveOrZero Long personVersion,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @PastOrPresent @Nullable LocalDate dateOfBirth,
        @Size(max = 30) @Nullable String phone,
        @Email @Size(max = 320) @Nullable String email,
        @Nullable DonorType type,
        @Pattern(regexp = "^([A-Za-z]{5}[0-9]{4}[A-Za-z])?$", message = "must look like ABCDE1234F, or be empty")
        @Nullable String pan,
        @Nullable Boolean wantsTaxReceipt,
        @Nullable Boolean receivesUpdates) {
}
