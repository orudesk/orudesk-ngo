package com.orudesk.orudesk.donations.application.port.in;

import java.util.List;

/**
 * Everything one donor gave, newest first, and the total.
 *
 * @param total "12500.00": added up by the database, in NUMERIC, never in floating point
 */
public record DonorDonations(List<DonationDetails> donations, String total, String totalFormatted) {

    public DonorDonations {
        donations = List.copyOf(donations);
    }
}
