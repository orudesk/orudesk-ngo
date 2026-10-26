package com.orudesk.orudesk.donors.application.port.out;

import com.orudesk.orudesk.donors.domain.Donor;
import com.orudesk.orudesk.donors.domain.DonorId;

import java.util.Optional;

public interface LoadDonorPort {

    Optional<Donor> findById(DonorId id);
}
