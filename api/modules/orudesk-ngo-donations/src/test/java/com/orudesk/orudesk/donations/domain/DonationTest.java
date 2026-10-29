package com.orudesk.orudesk.donations.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DonationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 29);
    private static final UUID DONOR = UUID.randomUUID();

    @Test
    void cashBelowTwoLakhIsFineAndNeedsNoReference() {
        Donation donation = Donation.record(DONOR, Amount.of("199999.99"), TODAY, PaymentMode.CASH, " ", "General", TODAY);

        assertThat(donation.reference()).isNull();
        assertThat(donation.purpose()).isEqualTo("General");
    }

    @Test
    void cashOfTwoLakhOrMoreIsRefused() {
        assertThatThrownBy(() -> Donation.record(DONOR, Amount.rupees(200_000), TODAY, PaymentMode.CASH, null, null, TODAY))
                .hasMessageContaining("269ST");
    }

    @Test
    void aChequeNeedsItsNumber() {
        assertThatThrownBy(() -> Donation.record(DONOR, Amount.rupees(5000), TODAY, PaymentMode.CHEQUE, "", null, TODAY))
                .hasMessageContaining("cheque payment needs its reference");
        assertThat(Donation.record(DONOR, Amount.rupees(5000), TODAY, PaymentMode.CHEQUE, "004512", null, TODAY)
                .reference()).isEqualTo("004512");
    }

    @Test
    void moneyCantArriveTomorrow() {
        assertThatThrownBy(() -> Donation.record(DONOR, Amount.rupees(500), TODAY.plusDays(1), PaymentMode.UPI, "UTR1", null, TODAY))
                .hasMessageContaining("future");
    }

    @Test
    void aCancelledDonationKeepsItsReceiptAndCantBeCancelledTwice() {
        Donation donation = Donation.record(DONOR, Amount.rupees(5000), TODAY, PaymentMode.CHEQUE, "004512", null, TODAY)
                .issueReceipt(new ReceiptNumber(new FinancialYear(2026), 3), TODAY);

        Donation cancelled = donation.cancel(new Cancellation(TODAY, "  cheque bounced "));

        assertThat(cancelled.isCancelled()).isTrue();
        assertThat(cancelled.cancellation().reason()).isEqualTo("cheque bounced");
        assertThat(cancelled.receiptNumber()).isEqualTo(donation.receiptNumber());
        assertThatThrownBy(() -> cancelled.cancel(new Cancellation(TODAY, "again")))
                .isInstanceOf(DonationAlreadyCancelledException.class);
        assertThatThrownBy(() -> new Cancellation(TODAY, " ")).hasMessageContaining("say why");
    }
}
