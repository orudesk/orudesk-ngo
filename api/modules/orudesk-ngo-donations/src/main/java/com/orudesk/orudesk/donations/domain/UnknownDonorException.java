package com.orudesk.orudesk.donations.domain;

import java.util.UUID;

/** A donation names a donor that isn't in this NGO's records. */
public class UnknownDonorException extends RuntimeException {

    public UnknownDonorException(UUID donorId) {
        super("no donor " + donorId);
    }
}
