package com.orudesk.orudesk.donations.application.port.out;

import com.orudesk.orudesk.donations.domain.FinancialYear;
import com.orudesk.orudesk.donations.domain.ReceiptNumber;

/** Hands out receipt numbers, one series per NGO and financial year, without gaps (Day 57). */
public interface ReceiptNumbers {

    /** The next number of the year. Taken back if the caller's transaction rolls back, so no number is skipped. */
    ReceiptNumber next(FinancialYear year);
}
