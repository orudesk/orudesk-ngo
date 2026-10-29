package com.orudesk.orudesk.donations.application.port.in;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A donation as the API shows it.
 *
 * @param amount          "1500.00", a string so that no JSON parser rounds it
 * @param amountFormatted "₹1,500.00", for people
 * @param receiptNumber   "2026-27/000042" (Day 57)
 * @param cancelledOn     set when the donation was cancelled (Day 58), with cancelReason
 * @param version         send it back to cancel: a change in between is a conflict
 */
public record DonationDetails(UUID id, UUID donorId, String donorName, String amount, String amountFormatted,
                              LocalDate receivedOn, String mode, @Nullable String reference, @Nullable String purpose,
                              String receiptNumber, @Nullable LocalDate cancelledOn, @Nullable String cancelReason,
                              long version) {
}
