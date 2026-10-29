package com.orudesk.orudesk.donations.application.port.out;

import com.orudesk.orudesk.donations.domain.Donation;

public interface SaveDonationPort {

    void save(Donation donation);

    /** Writes a cancellation (Day 58). Throws an optimistic locking failure if someone changed it first. */
    void update(Donation donation);
}
