package com.orudesk.orudesk.donations.application.port.in;

import java.util.UUID;

/** @param version the donation's version that was read: someone else's change in between is a conflict */
public record CancelDonationCommand(UUID donationId, long version, String reason) {
}
