package com.orudesk.orudesk.donations.domain;

import java.util.Objects;
import java.util.UUID;

public record DonationId(UUID value) {

    public DonationId {
        Objects.requireNonNull(value, "donation id must not be null");
    }

    public static DonationId newId() {
        return new DonationId(UUID.randomUUID());
    }
}
