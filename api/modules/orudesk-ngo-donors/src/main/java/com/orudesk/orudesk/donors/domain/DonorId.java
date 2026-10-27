package com.orudesk.orudesk.donors.domain;

import java.util.Objects;
import java.util.UUID;

public record DonorId(UUID value) {

    public DonorId {
        Objects.requireNonNull(value, "donor id must not be null");
    }

    public static DonorId newId() {
        return new DonorId(UUID.randomUUID());
    }
}
