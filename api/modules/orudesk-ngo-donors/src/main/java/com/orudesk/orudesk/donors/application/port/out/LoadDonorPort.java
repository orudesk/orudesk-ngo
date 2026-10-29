package com.orudesk.orudesk.donors.application.port.out;

import com.orudesk.orudesk.donors.domain.Donor;
import com.orudesk.orudesk.donors.domain.DonorId;

import java.util.Optional;

/** The write side's read (Day 49): a donor as the domain needs it to change it, with the real PAN. */
public interface LoadDonorPort {

    Optional<Donor> findById(DonorId id);
}
