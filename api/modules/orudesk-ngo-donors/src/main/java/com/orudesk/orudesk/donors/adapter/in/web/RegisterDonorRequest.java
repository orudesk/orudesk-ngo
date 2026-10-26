package com.orudesk.orudesk.donors.adapter.in.web;

import com.orudesk.orudesk.donors.domain.DonorType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * The JSON body of POST /api/v1/donors.
 *
 * @param type            INDIVIDUAL when left out
 * @param wantsTaxReceipt Boolean, not boolean (and receivesUpdates too): Jackson 3 refuses a missing value
 *                        for a primitive, and leaving it out should simply mean "no"
 */
record RegisterDonorRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        LocalDate dateOfBirth,
        @Size(max = 30) String phone,
        @Email @Size(max = 320) String email,
        DonorType type,
        @Pattern(regexp = "^[A-Za-z]{5}[0-9]{4}[A-Za-z]$", message = "must look like ABCDE1234F") String pan,
        Boolean wantsTaxReceipt,
        Boolean receivesUpdates) {
}
