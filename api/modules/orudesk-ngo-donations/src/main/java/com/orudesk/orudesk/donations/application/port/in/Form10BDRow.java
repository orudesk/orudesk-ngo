package com.orudesk.orudesk.donations.application.port.in;

/**
 * One line of Form 10BD, the statement of donations an NGO with 80G registration files each year (Day 59): one
 * per donor and mode of receipt. The donor claims the deduction from what the NGO reports here (Form 10BE).
 *
 * @param modeOfReceipt "Cash" or "Electronic modes including account payee cheque/draft", the form's own words
 * @param donationType  "Others": Orudesk doesn't record corpus donations or specific grants yet
 */
public record Form10BDRow(int serialNumber, String pan, String donorName, String donationType, String modeOfReceipt,
                          String amount) {

    @Override
    public String toString() {
        return "Form10BDRow[" + serialNumber + ", " + amount + "]";
    }
}
