package com.orudesk.orudesk.donors.application.port.in;

import com.orudesk.orudesk.donors.domain.DonorType;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A donor as the API shows it: the registry's details and the donor's, with the PAN masked.
 *
 * @param version       the donor's version, and personVersion the person's: an edit sends both back (Day 49)
 */
public record DonorDetails(UUID id, UUID personId, String firstName, String lastName, @Nullable LocalDate dateOfBirth,
                           @Nullable String phone, @Nullable String email,
                           DonorType type, @Nullable String panMasked, boolean wantsTaxReceipt, boolean receivesUpdates,
                           LocalDate registeredOn, long version, long personVersion) {
}
