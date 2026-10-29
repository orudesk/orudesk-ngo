package com.orudesk.orudesk.donations.application.port.out;

import com.orudesk.orudesk.donations.domain.Donation;

public interface SaveDonationPort {

    void save(Donation donation);
}
