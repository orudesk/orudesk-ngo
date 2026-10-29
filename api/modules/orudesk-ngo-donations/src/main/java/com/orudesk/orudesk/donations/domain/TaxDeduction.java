package com.orudesk.orudesk.donations.domain;

import org.jspecify.annotations.Nullable;

/**
 * Can the donor claim this donation under section 80G (Day 57)? The receipt says so, and says why not when it
 * can't: a donor shouldn't learn at tax time that their receipt was worthless.
 *
 * @param reason empty when eligible
 */
public record TaxDeduction(boolean eligible, String reason) {

    /** Section 80G(5D): cash above two thousand rupees can't be deducted. */
    public static final Amount CASH_DEDUCTION_LIMIT = Amount.rupees(2_000);

    public static TaxDeduction decide(Donation donation, boolean donorWantsIt, @Nullable String donorPan,
                                      @Nullable String registration80G) {
        if (registration80G == null || registration80G.isBlank()) {
            return no("the organisation has no 80G registration on file");
        }
        if (!donorWantsIt) {
            return no("the donor didn't ask for a tax receipt");
        }
        if (donorPan == null) {
            return no("the donor has no PAN on file");
        }
        if (donation.mode() == PaymentMode.CASH && donation.amount().compareTo(CASH_DEDUCTION_LIMIT) > 0) {
            return no("cash above " + CASH_DEDUCTION_LIMIT + " can't be deducted under 80G");
        }
        return new TaxDeduction(true, "");
    }

    private static TaxDeduction no(String reason) {
        return new TaxDeduction(false, reason);
    }
}
