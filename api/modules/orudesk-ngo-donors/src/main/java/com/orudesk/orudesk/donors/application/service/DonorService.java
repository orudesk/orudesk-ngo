package com.orudesk.orudesk.donors.application.service;

import com.orudesk.orudesk.donors.application.port.in.DonorDetails;
import com.orudesk.orudesk.donors.application.port.in.GetDonorQuery;
import com.orudesk.orudesk.donors.application.port.in.RegisterDonorCommand;
import com.orudesk.orudesk.donors.application.port.in.RegisterDonorUseCase;
import com.orudesk.orudesk.donors.application.port.out.LoadDonorPort;
import com.orudesk.orudesk.donors.application.port.out.SaveDonorPort;
import com.orudesk.orudesk.donors.domain.Donor;
import com.orudesk.orudesk.donors.domain.DonorId;
import com.orudesk.orudesk.donors.domain.DonorNotFoundException;
import com.orudesk.orudesk.donors.domain.Pan;
import com.orudesk.orudesk.party.application.port.in.GetPersonQuery;
import com.orudesk.orudesk.party.application.port.in.PersonDetails;
import com.orudesk.orudesk.party.application.port.in.RegisterPersonCommand;
import com.orudesk.orudesk.party.application.port.in.RegisterPersonUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Service
class DonorService implements RegisterDonorUseCase, GetDonorQuery {

    private final RegisterPersonUseCase registerPerson;
    private final GetPersonQuery getPerson;
    private final SaveDonorPort saveDonorPort;
    private final LoadDonorPort loadDonorPort;
    private final Clock clock;

    DonorService(RegisterPersonUseCase registerPerson, GetPersonQuery getPerson,
                 SaveDonorPort saveDonorPort, LoadDonorPort loadDonorPort, Clock clock) {
        this.registerPerson = registerPerson;
        this.getPerson = getPerson;
        this.saveDonorPort = saveDonorPort;
        this.loadDonorPort = loadDonorPort;
        this.clock = clock;
    }

    /**
     * One transaction for both inserts. Party's register() is @Transactional too, and by default it joins
     * this transaction instead of starting its own: if saving the donor fails, the person is rolled back
     * with it, and the registry never gets a "donor" who isn't one.
     */
    @Override
    @Transactional
    public UUID register(RegisterDonorCommand command) {
        Pan pan = command.pan() == null || command.pan().isBlank() ? null : new Pan(command.pan());

        UUID personId = registerPerson.register(new RegisterPersonCommand(
                command.firstName(), command.lastName(), command.dateOfBirth(), command.phone(), command.email()));

        Donor donor = Donor.register(personId, pan, command.receivesUpdates(), LocalDate.now(clock));
        saveDonorPort.save(donor);
        return donor.id().value();
    }

    @Override
    @Transactional(readOnly = true)
    public DonorDetails getDonor(UUID donorId) {
        DonorId id = new DonorId(donorId);
        Donor donor = loadDonorPort.findById(id).orElseThrow(() -> new DonorNotFoundException(id));
        PersonDetails person = getPerson.getPerson(donor.personId());
        return new DonorDetails(donor.id().value(), donor.personId(),
                person.firstName(), person.lastName(), person.phone(), person.email(),
                donor.pan() == null ? null : donor.pan().masked(),
                donor.receivesUpdates(), donor.registeredOn());
    }
}
