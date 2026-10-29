package com.orudesk.orudesk.donations.application.port.in;

import com.orudesk.orudesk.shared.AuditableEvent;

import java.util.UUID;

/** Money was received (Day 56). Audited on the donation. */
public record DonationRecorded(UUID donationId, UUID donorId, String donorName, String amountFormatted)
        implements AuditableEvent {

    @Override public String action() { return "donations.donation.recorded"; }
    @Override public String subjectType() { return "donation"; }
    @Override public UUID subjectId() { return donationId; }
    @Override public String summary() { return "Recorded a donation of " + amountFormatted + " from " + donorName; }
}
