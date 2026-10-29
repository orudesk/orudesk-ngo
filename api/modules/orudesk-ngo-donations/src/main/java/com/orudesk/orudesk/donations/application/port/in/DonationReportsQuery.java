package com.orudesk.orudesk.donations.application.port.in;

import java.util.List;

/** Reports over a financial year, such as "2026-27" (Day 59). */
public interface DonationReportsQuery {

    DonationSummary summary(String financialYear);

    /**
     * The deductible donations of the year, for Form 10BD. Throws IllegalArgumentException if the NGO has no 80G
     * registration: without one there is nothing to file.
     */
    List<Form10BDRow> form10BD(String financialYear);
}
