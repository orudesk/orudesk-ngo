package com.orudesk.orudesk.donations.application.port.in;

import java.util.UUID;

public interface GetDonationQuery {

    /** Throws DonationNotFoundException if there's no such donation in this tenant. */
    DonationDetails getDonation(UUID donationId);

    /** The receipt, as printed (Day 57). Throws DonationNotFoundException. */
    DonationReceipt receiptFor(UUID donationId);

    /** Throws UnknownDonorException if there's no such donor in this tenant. */
    DonorDonations donationsOf(UUID donorId);
}
