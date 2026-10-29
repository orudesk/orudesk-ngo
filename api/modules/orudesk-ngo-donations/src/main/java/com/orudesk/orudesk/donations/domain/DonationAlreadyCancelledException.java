package com.orudesk.orudesk.donations.domain;

public class DonationAlreadyCancelledException extends RuntimeException {

    public DonationAlreadyCancelledException(DonationId id) {
        super("donation " + id.value() + " is already cancelled");
    }
}
