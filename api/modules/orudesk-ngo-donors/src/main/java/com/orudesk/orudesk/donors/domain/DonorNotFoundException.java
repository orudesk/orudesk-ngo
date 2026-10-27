package com.orudesk.orudesk.donors.domain;

public class DonorNotFoundException extends RuntimeException {

    public DonorNotFoundException(DonorId id) {
        super("No donor with id " + id.value());
    }
}
