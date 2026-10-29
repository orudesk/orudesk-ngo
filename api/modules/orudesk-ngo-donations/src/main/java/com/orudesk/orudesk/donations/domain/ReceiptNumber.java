package com.orudesk.orudesk.donations.domain;

import java.util.Objects;

/**
 * "2026-27/000042": the 42nd receipt of the financial year (Day 57). Numbered without gaps, per NGO and year,
 * because an auditor checks that every number in the book is accounted for: a missing one looks like a receipt
 * that was torn out.
 */
public record ReceiptNumber(FinancialYear year, int sequence) {

    public ReceiptNumber {
        Objects.requireNonNull(year);
        if (sequence < 1 || sequence > 999_999) {
            throw new IllegalArgumentException("a receipt's number is 1 to 999999 within its year");
        }
    }

    public static ReceiptNumber parse(String value) {
        int slash = value.indexOf('/');
        if (slash < 0) {
            throw new IllegalArgumentException("not a receipt number: " + value);
        }
        return new ReceiptNumber(FinancialYear.parse(value.substring(0, slash)), Integer.parseInt(value.substring(slash + 1)));
    }

    public String value() {
        return year.label() + "/" + String.format("%06d", sequence);
    }

    @Override
    public String toString() {
        return value();
    }
}
