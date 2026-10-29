package com.orudesk.orudesk.donations.application.port.out;

import com.orudesk.orudesk.donations.domain.Amount;
import com.orudesk.orudesk.donations.domain.Donation;
import com.orudesk.orudesk.donations.domain.DonationId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadDonationPort {

    Optional<Donation> findById(DonationId id);

    /** Newest first. */
    List<Donation> findByDonor(UUID donorId);

    /** The sum of the donor's donations; empty if they gave nothing yet. */
    Optional<Amount> totalOf(UUID donorId);
}
