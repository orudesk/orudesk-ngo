package com.orudesk.orudesk.donors.application.port.in;

import com.orudesk.orudesk.donors.domain.DonorType;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Everything on the donor's form, as it should now be (a replace, like PUT), with the two versions it was read
 * at: the donor's and the person's (DonorDetails.version and personVersion).
 *
 * @param pan the one field that isn't replaced: the form only ever saw it masked, so null keeps the PAN as it
 *            is, a blank removes it, and a value replaces it
 */
public record EditDonorCommand(UUID donorId, long version, long personVersion, String firstName, String lastName,
                               LocalDate dateOfBirth, String phone, String email, DonorType type, String pan,
                               boolean wantsTaxReceipt, boolean receivesUpdates) {
}
