package com.orudesk.orudesk.donations.application.port.out;

import com.orudesk.orudesk.donations.domain.FinancialYear;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Sums over a financial year, computed by the database (Day 59). Cancelled donations are always left out. */
public interface DonationTotals {

    /** @param key a month ("2026-04") or a payment mode ("UPI") */
    record Total(String key, BigDecimal amount, long count) {
    }

    /** @param cash true for the cash line, false for every other mode */
    record DonorTotal(UUID donorId, boolean cash, BigDecimal amount) {
    }

    List<Total> byMonth(FinancialYear year);

    List<Total> byMode(FinancialYear year);

    long cancelledCount(FinancialYear year);

    /**
     * Per donor, cash and non-cash apart, only what 80G allows: cash donations above two thousand rupees are left
     * out (they can't be deducted). Whether the donor has a PAN and asked for receipts is the donors module's to say.
     */
    List<DonorTotal> deductibleByDonor(FinancialYear year);
}
