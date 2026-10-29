package com.orudesk.orudesk.donations.domain;

/** How the money came. Stored by name, so adding a mode never renumbers existing rows. */
public enum PaymentMode {
    CASH,
    CHEQUE,
    BANK_TRANSFER,   // NEFT, RTGS, IMPS
    UPI,
    CARD,
    ONLINE;          // a payment gateway on the NGO's website

    /** A cheque has a number, a transfer a UTR, a UPI payment a transaction id: the bank statement shows it. */
    public boolean needsReference() {
        return this != CASH;
    }
}
