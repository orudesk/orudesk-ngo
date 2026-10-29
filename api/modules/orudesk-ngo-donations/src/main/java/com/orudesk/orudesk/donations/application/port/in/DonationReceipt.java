package com.orudesk.orudesk.donations.application.port.in;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Everything printed on a donation's receipt (Day 57).
 *
 * @param donorPan        the full PAN, but only on a receipt that's eligible for 80G (the deduction needs it);
 *                        otherwise null, so a receipt never shows a PAN it doesn't need
 * @param taxDeductible   true if the donor can claim it under section 80G
 * @param notDeductibleBecause why not, in words; empty when deductible
 * @param cancelledOn     set if the donation was cancelled (Day 58): the receipt is void, and says so
 */
public record DonationReceipt(UUID donationId, String receiptNumber, String financialYear, LocalDate issuedOn,
                              String organisationName, @Nullable String organisationAddress,
                              @Nullable String organisationPan, @Nullable String registration80G,
                              String donorName, @Nullable String donorPan,
                              String amount, String amountFormatted, String amountInWords,
                              LocalDate receivedOn, String mode, @Nullable String reference, @Nullable String purpose,
                              boolean taxDeductible, String notDeductibleBecause,
                              @Nullable LocalDate cancelledOn, @Nullable String cancelReason) {

    @Override
    public String toString() {
        return "DonationReceipt[" + receiptNumber + ", " + amountFormatted + "]";
    }
}
