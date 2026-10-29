package com.orudesk.orudesk.donations.application.port.in;

import com.orudesk.orudesk.shared.AuditableEvent;

import java.util.UUID;

/** A donation was cancelled (Day 58). The reason goes into the audit log's summary. */
public record DonationCancelled(UUID donationId, String receiptNumber, String amountFormatted, String reason)
        implements AuditableEvent {

    @Override public String action() { return "donations.donation.cancelled"; }
    @Override public String subjectType() { return "donation"; }
    @Override public UUID subjectId() { return donationId; }
    @Override public String summary() { return "Cancelled receipt " + receiptNumber + " (" + amountFormatted + "): " + reason; }
}
