package com.orudesk.orudesk.donors.application.port.in;

import com.orudesk.orudesk.donors.domain.DonorType;

import java.time.LocalDate;

/** A new donor: the person's details for the registry, and the donor's own. Only the names are required. */
public record RegisterDonorCommand(String firstName, String lastName, LocalDate dateOfBirth, String phone,
                                   String email, DonorType type, String pan, boolean wantsTaxReceipt,
                                   boolean receivesUpdates) {
}
