package com.orudesk.orudesk.donations.application.port.in;

import java.util.List;

/**
 * A financial year at a glance (Day 59). Amounts are strings ("125000.00"), summed by PostgreSQL in NUMERIC.
 * Cancelled donations are left out of every total and counted separately.
 *
 * @param byMonth all twelve months, April first, with zero for a month without donations
 */
public record DonationSummary(String financialYear, String total, String totalFormatted, long count,
                              long cancelledCount, List<Line> byMonth, List<Line> byMode) {

    public DonationSummary {
        byMonth = List.copyOf(byMonth);
        byMode = List.copyOf(byMode);
    }

    /** @param key "2026-04" for a month, "UPI" for a payment mode */
    public record Line(String key, String total, long count) {
    }
}
