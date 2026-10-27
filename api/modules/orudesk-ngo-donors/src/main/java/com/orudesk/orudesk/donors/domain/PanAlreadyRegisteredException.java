package com.orudesk.orudesk.donors.domain;

/** Two donors with one PAN would mean two tax receipts for one taxpayer. Usually it's the same person twice. */
public class PanAlreadyRegisteredException extends RuntimeException {

    public PanAlreadyRegisteredException(Pan pan) {
        super("A donor with PAN " + pan.masked() + " is already registered");
    }
}
