package com.orudesk.orudesk.donors.application.port.out;

import com.orudesk.orudesk.donors.domain.Donor;

public interface SaveDonorPort {

    /** @throws com.orudesk.orudesk.donors.domain.PanAlreadyRegisteredException if another donor has the PAN */
    void save(Donor donor);

    /**
     * Writes new details if the stored row is still at donor.version(); returns the version after the save.
     *
     * @throws com.orudesk.orudesk.donors.domain.PanAlreadyRegisteredException if another donor has the new PAN
     */
    long update(Donor donor);
}
