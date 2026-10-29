package com.orudesk.orudesk.donors.adapter.out.persistence;

import com.orudesk.orudesk.donors.application.port.out.LoadDonorPort;
import com.orudesk.orudesk.donors.application.port.out.SaveDonorPort;
import com.orudesk.orudesk.donors.domain.Donor;
import com.orudesk.orudesk.donors.domain.DonorId;
import com.orudesk.orudesk.donors.domain.Pan;
import com.orudesk.orudesk.donors.domain.PanAlreadyRegisteredException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class DonorPersistenceAdapter implements SaveDonorPort, LoadDonorPort {

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
            throw panTakenOr(e, donor);
        }
    }

    @Override
    public long update(Donor donor) {
        DonorJpaEntity row = donors.findById(donor.id().value())
                .orElseThrow(() -> new IllegalStateException("updating a donor that isn't there: " + donor.id().value()));
        if (row.getVersion() != donor.version()) {
            throw new ObjectOptimisticLockingFailureException(DonorJpaEntity.class, donor.id().value());
        }
        row.changeDetails(donor.type(), donor.pan() == null ? null : donor.pan().value(), donor.wantsTaxReceipt(),
                donor.receivesUpdates());
        try {
            donors.flush();   // the UPDATE ... WHERE version = ? runs now: a lost race or a taken PAN throws here
        } catch (DataIntegrityViolationException e) {
            throw panTakenOr(e, donor);
        }
        return row.getVersion();
    }

    @Override
    public Optional<Donor> findById(DonorId id) {
        return donors.findById(id.value())
                .map(row -> Donor.restore(id, row.getPersonId(), row.getDonorType(),
                        row.getPan() == null ? null : new Pan(row.getPan()), row.isWantsTaxReceipt(),
                        row.isReceivesUpdates(), row.getRegisteredOn(), row.getVersion()));
    }

    private static RuntimeException panTakenOr(DataIntegrityViolationException e, Donor donor) {
        if (donor.pan() != null && String.valueOf(e.getMessage()).contains("uk_ngo_donor_pan")) {
            return new PanAlreadyRegisteredException(donor.pan());
        }
        return e;
    }
}
