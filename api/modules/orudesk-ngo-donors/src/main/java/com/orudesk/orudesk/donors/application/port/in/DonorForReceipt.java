package com.orudesk.orudesk.donors.application.port.in;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * What a donation receipt needs to know about its donor (Day 56). Plain types only, so the donations module
 * doesn't depend on the donors module's domain classes.
 *
 * @param pan the full PAN, not masked: an 80G receipt must print it. Only for receipts and the annual statement.
 */
public record DonorForReceipt(UUID donorId, String fullName, @Nullable String email, @Nullable String pan,
                              boolean wantsTaxReceipt) {

    @Override
    public String toString() {
        return "DonorForReceipt[donorId=" + donorId + ", fullName=" + fullName + ", pan=" + (pan == null ? null : "****") + "]";
    }
}
