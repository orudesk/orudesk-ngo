package com.orudesk.orudesk.donors.application.port.out;

import com.orudesk.orudesk.donors.domain.Donor;

public interface SaveDonorPort {

    /** @throws com.orudesk.orudesk.donors.domain.PanAlreadyRegisteredException if another donor has the PAN */
    void save(Donor donor);
}
