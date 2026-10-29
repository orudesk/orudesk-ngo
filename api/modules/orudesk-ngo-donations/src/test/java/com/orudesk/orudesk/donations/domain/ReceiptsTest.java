package com.orudesk.orudesk.donations.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReceiptsTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 29);

    @Test
    void theFinancialYearRunsFromAprilToMarch() {
        assertThat(FinancialYear.of(LocalDate.of(2026, 3, 31)).label()).isEqualTo("2025-26");
        assertThat(FinancialYear.of(LocalDate.of(2026, 4, 1)).label()).isEqualTo("2026-27");
        assertThat(FinancialYear.of(LocalDate.of(2099, 12, 1)).label()).isEqualTo("2099-00");
        assertThat(FinancialYear.parse("2026-27").lastDay()).isEqualTo(LocalDate.of(2027, 3, 31));
        assertThatThrownBy(() -> FinancialYear.parse("2026-28")).hasMessageContaining("did you mean 2026-27");
    }

    @Test
    void aReceiptNumberIsTheYearAndASixDigitSequence() {
        ReceiptNumber number = new ReceiptNumber(new FinancialYear(2026), 42);
        assertThat(number.value()).isEqualTo("2026-27/000042");
        assertThat(ReceiptNumber.parse("2026-27/000042")).isEqualTo(number);
    }

    @Test
    void aDonationGetsOneReceiptFromItsOwnYear() {
        Donation donation = Donation.record(UUID.randomUUID(), Amount.rupees(500), LocalDate.of(2026, 3, 30),
                PaymentMode.CASH, null, null, TODAY);

        assertThatThrownBy(() -> donation.issueReceipt(new ReceiptNumber(new FinancialYear(2026), 1), TODAY))
                .hasMessageContaining("not from 2025-26");
        Donation receipted = donation.issueReceipt(new ReceiptNumber(new FinancialYear(2025), 7), TODAY);
        assertThat(receipted.receiptNumber().value()).isEqualTo("2025-26/000007");
        assertThatThrownBy(() -> receipted.issueReceipt(new ReceiptNumber(new FinancialYear(2025), 8), TODAY))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void theAmountIsWrittenInIndianWords() {
        assertThat(RupeesInWords.of(Amount.of("1500"))).isEqualTo("Rupees One Thousand Five Hundred Only");
        assertThat(RupeesInWords.of(Amount.of("150000.50"))).isEqualTo("Rupees One Lakh Fifty Thousand and Fifty Paise Only");
        assertThat(RupeesInWords.of(Amount.of("12345678.09")))
                .isEqualTo("Rupees One Crore Twenty Three Lakh Forty Five Thousand Six Hundred Seventy Eight and Nine Paise Only");
        assertThat(RupeesInWords.of(Amount.of("0.75"))).isEqualTo("Rupees Zero and Seventy Five Paise Only");
        assertThat(RupeesInWords.of(Amount.of("12000000000"))).isEqualTo("Rupees One Thousand Two Hundred Crore Only");
    }

    @Test
    void the80GDeductionSaysWhyNot() {
        Donation cash = Donation.record(UUID.randomUUID(), Amount.rupees(2_001), TODAY, PaymentMode.CASH, null, null, TODAY);
        Donation upi = Donation.record(UUID.randomUUID(), Amount.rupees(2_001), TODAY, PaymentMode.UPI, "UTR", null, TODAY);

        assertThat(TaxDeduction.decide(upi, true, "ABCDE1234F", "AAATN1234FF20214").eligible()).isTrue();
        assertThat(TaxDeduction.decide(cash, true, "ABCDE1234F", "AAATN1234FF20214").reason()).contains("cash above ₹2,000.00");
        assertThat(TaxDeduction.decide(upi, true, null, "AAATN1234FF20214").reason()).contains("no PAN");
        assertThat(TaxDeduction.decide(upi, false, "ABCDE1234F", "AAATN1234FF20214").reason()).contains("didn't ask");
        assertThat(TaxDeduction.decide(upi, true, "ABCDE1234F", null).reason()).contains("no 80G registration");
    }
}
