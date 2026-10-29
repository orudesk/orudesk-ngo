package com.orudesk.orudesk.donations.application.port.in;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.UUID;

/**
 * @param amount rupees as text, such as "1500.00": exact, never a floating-point number
 * @param mode   CASH, CHEQUE, BANK_TRANSFER, UPI, CARD or ONLINE
 */
public record RecordDonationCommand(UUID donorId, String amount, LocalDate receivedOn, String mode,
                                    @Nullable String reference, @Nullable String purpose) {
}
