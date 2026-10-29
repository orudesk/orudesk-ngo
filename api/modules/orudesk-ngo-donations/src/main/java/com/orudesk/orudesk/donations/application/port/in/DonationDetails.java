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
 */
public record DonationDetails(UUID id, UUID donorId, String donorName, String amount, String amountFormatted,
                              LocalDate receivedOn, String mode, @Nullable String reference, @Nullable String purpose,
                              String receiptNumber) {
}
