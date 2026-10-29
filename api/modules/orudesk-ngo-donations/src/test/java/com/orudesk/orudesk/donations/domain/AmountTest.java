package com.orudesk.orudesk.donations.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AmountTest {

    @Test
    void isExactAndAlwaysHasTwoDecimals() {
        assertThat(Amount.of("1500").plain()).isEqualTo("1500.00");
        assertThat(Amount.of("0.10").value().add(Amount.of("0.20").value())).isEqualByComparingTo("0.30");
        assertThat(Amount.of("1500.5")).isEqualTo(Amount.of("1500.50"));
    }

    @Test
    void refusesWhatIsNotAnAmountOfRupees() {
        assertThatThrownBy(() -> Amount.of("0")).hasMessageContaining("more than zero");
        assertThatThrownBy(() -> Amount.of("-5")).hasMessageContaining("more than zero");
        assertThatThrownBy(() -> Amount.of("10.005")).hasMessageContaining("two decimals");
        assertThatThrownBy(() -> Amount.of("ten")).hasMessageContaining("not an amount");
        assertThatThrownBy(() -> new Amount(new BigDecimal("1000000000000"))).hasMessageContaining("crore");
    }

    @Test
    void isWrittenTheIndianWay() {
        assertThat(Amount.of("100").formatted()).isEqualTo("₹100.00");
        assertThat(Amount.of("1500").formatted()).isEqualTo("₹1,500.00");
        assertThat(Amount.of("150000").formatted()).isEqualTo("₹1,50,000.00");
        assertThat(Amount.of("12345678.9").formatted()).isEqualTo("₹1,23,45,678.90");
    }
}
