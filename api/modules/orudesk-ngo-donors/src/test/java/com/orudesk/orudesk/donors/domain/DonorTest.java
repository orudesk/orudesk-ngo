package com.orudesk.orudesk.donors.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DonorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 9);

    @Test
    void aTaxReceiptNeedsAPan() {
        assertThatThrownBy(() -> Donor.register(UUID.randomUUID(), DonorType.INDIVIDUAL, null, true, false, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("a tax receipt needs a PAN");
    }

    @Test
    void withAPanATaxReceiptIsFine() {
        Donor donor = Donor.register(UUID.randomUUID(), DonorType.CORPORATE, new Pan("ABCDE1234F"), true, false, TODAY);

        assertThat(donor.wantsTaxReceipt()).isTrue();
        assertThat(donor.type()).isEqualTo(DonorType.CORPORATE);
    }

    @Test
    void changingDetailsKeepsTheRuleThatAReceiptNeedsAPan() {
        Donor donor = Donor.register(UUID.randomUUID(), DonorType.INDIVIDUAL, new Pan("ABCDE1234F"), true, false, TODAY);

        assertThatThrownBy(() -> donor.changeDetails(DonorType.INDIVIDUAL, null, true, false))
                .isInstanceOf(IllegalArgumentException.class);

        Donor changed = donor.changeDetails(DonorType.FOUNDATION, null, false, true);
        assertThat(changed.type()).isEqualTo(DonorType.FOUNDATION);
        assertThat(changed.pan()).isNull();
        assertThat(changed.id()).isEqualTo(donor.id());
        assertThat(changed.version()).isEqualTo(donor.version());   // the database moves the version, not the domain
    }
}
