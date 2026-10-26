package com.orudesk.orudesk.donors.application.port.in;

import java.util.List;

public interface CountDonorsQuery {

    /** One line per donor type that has donors, in the order of DonorType. */
    List<DonorCount> countByType();
}
