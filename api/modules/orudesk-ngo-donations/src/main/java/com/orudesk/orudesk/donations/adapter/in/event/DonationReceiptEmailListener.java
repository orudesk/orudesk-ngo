package com.orudesk.orudesk.donations.adapter.in.event;

import com.orudesk.orudesk.donations.application.port.in.DonationReceipt;
import com.orudesk.orudesk.donations.application.port.in.DonationRecorded;
import com.orudesk.orudesk.donations.application.port.in.GetDonationQuery;
import com.orudesk.orudesk.donors.application.port.in.DonorForReceipt;
import com.orudesk.orudesk.donors.application.port.in.DonorReceiptQuery;
import com.orudesk.orudesk.shared.EmailMessage;
import com.orudesk.orudesk.shared.EmailSender;
import com.orudesk.orudesk.shared.ProcessedEvents;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Thanks the donor by email, with the receipt (Day 58), after the donation commits: through the core's outbox, so
 * a mail server that's down delays the email instead of failing the donation, and the core's retry sends it later.
 *
 * Runs on another thread with nobody logged in: it reads by the ids in the event, never "the current user".
 */
@Component
class DonationReceiptEmailListener {

    private final GetDonationQuery donations;
    private final DonorReceiptQuery donors;
    private final EmailSender emailSender;
    private final ProcessedEvents processed;

    DonationReceiptEmailListener(GetDonationQuery donations, DonorReceiptQuery donors, EmailSender emailSender,
                                 ProcessedEvents processed) {
        this.donations = donations;
        this.donors = donors;
        this.emailSender = emailSender;
        this.processed = processed;
    }

    @ApplicationModuleListener
    void on(DonationRecorded event) {
        String email = donors.findForReceipt(event.donorId()).map(DonorForReceipt::email).orElse(null);
        if (email == null || email.isBlank()) {
            return;   // no address: the receipt is printed or downloaded from the donor's page instead
        }
        // One receipt email per donation, even if this runs twice (the core's Day 54)
        if (!processed.firstTime("donation-receipt-email:" + event.donationId())) {
            return;
        }
        DonationReceipt receipt = donations.receiptFor(event.donationId());
        emailSender.send(new EmailMessage(email, "Thank you: receipt " + receipt.receiptNumber(), text(receipt)));
    }

    private static String text(DonationReceipt r) {
        StringBuilder text = new StringBuilder();
        text.append("Dear ").append(r.donorName()).append(",\n\n")
                .append("Thank you for your donation to ").append(r.organisationName()).append(".\n\n")
                .append("Receipt number: ").append(r.receiptNumber()).append('\n')
                .append("Date received:  ").append(r.receivedOn()).append('\n')
                .append("Amount:         ").append(r.amountFormatted()).append('\n')
                .append("                ").append(r.amountInWords()).append('\n')
                .append("Paid by:        ").append(r.mode().replace('_', ' ').toLowerCase());
        if (r.reference() != null) {
            text.append(" (").append(r.reference()).append(')');
        }
        text.append('\n');
        if (r.purpose() != null) {
            text.append("Towards:        ").append(r.purpose()).append('\n');
        }
        text.append('\n');
        if (r.taxDeductible()) {
            text.append("This donation qualifies for deduction under section 80G of the Income Tax Act")
                    .append(" (registration ").append(r.registration80G()).append(").\n")
                    .append("Your PAN on this receipt: ").append(r.donorPan()).append('\n');
        } else {
            text.append("This receipt is not for a tax deduction: ").append(r.notDeductibleBecause()).append(".\n");
        }
        text.append("\n").append(r.organisationName());
        if (r.organisationAddress() != null) {
            text.append('\n').append(r.organisationAddress());
        }
        if (r.organisationPan() != null) {
            text.append("\nPAN ").append(r.organisationPan());
        }
        return text.append('\n').toString();
    }
}
