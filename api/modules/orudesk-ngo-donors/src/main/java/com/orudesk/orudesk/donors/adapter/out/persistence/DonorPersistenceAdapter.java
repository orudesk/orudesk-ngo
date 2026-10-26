package com.orudesk.orudesk.donors.adapter.out.persistence;

import com.orudesk.orudesk.donors.application.port.out.SaveDonorPort;
import com.orudesk.orudesk.donors.domain.Donor;
import com.orudesk.orudesk.donors.domain.PanAlreadyRegisteredException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
class DonorPersistenceAdapter implements SaveDonorPort {

    private final DonorJpaRepository donors;

    DonorPersistenceAdapter(DonorJpaRepository donors) {
        this.donors = donors;
    }

    @Override
    public void save(Donor donor) {
        try {
            // saveAndFlush: send the INSERT now, so a duplicate PAN fails here, where we can name it,
            // instead of at commit time, somewhere after the service method has returned
            donors.saveAndFlush(new DonorJpaEntity(donor.id().value(), donor.personId(), donor.type(),
                    donor.pan() == null ? null : donor.pan().value(), donor.wantsTaxReceipt(),
                    donor.receivesUpdates(), donor.registeredOn()));
        } catch (DataIntegrityViolationException e) {
            if (donor.pan() != null && String.valueOf(e.getMessage()).contains("uk_ngo_donor_pan")) {
                throw new PanAlreadyRegisteredException(donor.pan());
            }
            throw e;
        }
    }
}
