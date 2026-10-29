package com.orudesk.orudesk.donations.domain;

import java.time.LocalDate;
import java.time.Month;

/**
 * India's financial year, 1 April to 31 March (Day 57). Receipts are numbered per year, and the annual statement
 * to the tax department (Form 10BD) covers one.
 *
 * @param startYear 2026 for the year from 1 April 2026 to 31 March 2027
 */
public record FinancialYear(int startYear) {

    public FinancialYear {
        if (startYear < 2000 || startYear > 2999) {
            throw new IllegalArgumentException("not a financial year: " + startYear);
        }
    }

    public static FinancialYear of(LocalDate date) {
        return new FinancialYear(date.getMonth().compareTo(Month.APRIL) >= 0 ? date.getYear() : date.getYear() - 1);
    }

    /** From "2026-27", the way people and URLs write it. */
    public static FinancialYear parse(String label) {
        if (label == null || !label.matches("\\d{4}-\\d{2}")) {
            throw new IllegalArgumentException("a financial year looks like 2026-27");
        }
        FinancialYear year = new FinancialYear(Integer.parseInt(label.substring(0, 4)));
        if (!year.label().equals(label)) {
            throw new IllegalArgumentException("not a financial year: " + label + " (did you mean " + year.label() + "?)");
        }
        return year;
    }

    public LocalDate firstDay() {
        return LocalDate.of(startYear, Month.APRIL, 1);
    }

    public LocalDate lastDay() {
        return LocalDate.of(startYear + 1, Month.MARCH, 31);
    }

    /** "2026-27" */
    public String label() {
        return startYear + "-" + String.format("%02d", (startYear + 1) % 100);
    }

    @Override
    public String toString() {
        return label();
    }
}
