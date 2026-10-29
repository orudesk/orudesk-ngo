package com.orudesk.orudesk.donations.domain;

public class DonationNotFoundException extends RuntimeException {

    public DonationNotFoundException(DonationId id) {
        super("no donation " + id.value());
    }
}
