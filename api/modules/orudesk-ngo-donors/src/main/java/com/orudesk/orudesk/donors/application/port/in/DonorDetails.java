package com.orudesk.orudesk.donors.application.port.in;

import com.orudesk.orudesk.donors.domain.DonorType;

import java.time.LocalDate;
import java.util.UUID;

/** A donor as the API shows it: the registry's details and the donor's, with the PAN masked. */
public record DonorDetails(UUID id, UUID personId, String firstName, String lastName, String phone, String email,
                           DonorType type, String panMasked, boolean wantsTaxReceipt, boolean receivesUpdates,
                           LocalDate registeredOn) {
}
