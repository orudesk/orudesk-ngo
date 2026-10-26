package com.orudesk.orudesk.donors.application.service;

import com.orudesk.orudesk.donors.application.port.in.CountDonorsQuery;
import com.orudesk.orudesk.donors.application.port.in.DonorCount;
import com.orudesk.orudesk.donors.application.port.in.DonorDetails;
import com.orudesk.orudesk.donors.application.port.in.DonorListItem;
import com.orudesk.orudesk.donors.application.port.in.DonorPage;
import com.orudesk.orudesk.donors.application.port.in.DonorSearch;
import com.orudesk.orudesk.donors.application.port.in.GetDonorQuery;
import com.orudesk.orudesk.donors.application.port.in.RegisterDonorCommand;
import com.orudesk.orudesk.donors.application.port.in.RegisterDonorUseCase;
import com.orudesk.orudesk.donors.application.port.in.SearchDonorsQuery;
import com.orudesk.orudesk.donors.application.port.out.DonorPosition;
import com.orudesk.orudesk.donors.application.port.out.ReadDonorsPort;
import com.orudesk.orudesk.donors.application.port.out.SaveDonorPort;
import com.orudesk.orudesk.donors.domain.Donor;
import com.orudesk.orudesk.donors.domain.DonorId;
import com.orudesk.orudesk.donors.domain.DonorNotFoundException;
import com.orudesk.orudesk.donors.domain.DonorType;
import com.orudesk.orudesk.donors.domain.Pan;
import com.orudesk.orudesk.party.application.port.in.RegisterPersonCommand;
import com.orudesk.orudesk.party.application.port.in.RegisterPersonUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
class DonorService implements RegisterDonorUseCase, GetDonorQuery, SearchDonorsQuery, CountDonorsQuery {

    private final RegisterPersonUseCase registerPerson;
    private final SaveDonorPort saveDonorPort;
    private final ReadDonorsPort readDonorsPort;
    private final Clock clock;

    DonorService(RegisterPersonUseCase registerPerson, SaveDonorPort saveDonorPort, ReadDonorsPort readDonorsPort,
                 Clock clock) {
        this.registerPerson = registerPerson;
        this.saveDonorPort = saveDonorPort;
        this.readDonorsPort = readDonorsPort;
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
        DonorType type = command.type() == null ? DonorType.INDIVIDUAL : command.type();
        if (command.wantsTaxReceipt() && pan == null) {   // checked before the person is created, too
            throw new IllegalArgumentException("a tax receipt needs a PAN");
        }

        UUID personId = registerPerson.register(new RegisterPersonCommand(
                command.firstName(), command.lastName(), command.dateOfBirth(), command.phone(), command.email()));

        Donor donor = Donor.register(personId, type, pan, command.wantsTaxReceipt(), command.receivesUpdates(),
                LocalDate.now(clock));
        saveDonorPort.save(donor);
        return donor.id().value();
    }

    /** One query on the summary view, instead of the donor and then the person through party's API. */
    @Override
    @Transactional(readOnly = true)
    public DonorDetails getDonor(UUID donorId) {
        DonorId id = new DonorId(donorId);
        return readDonorsPort.findDetails(id).orElseThrow(() -> new DonorNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public DonorPage search(DonorSearch search) {
        String text = search.text() == null ? null : search.text().toLowerCase(Locale.ROOT);
        DonorPosition after = search.after() == null ? null : DonorCursor.decode(search.after());

        // One more than a page: if it comes back, there is a next page (Day 16)
        List<DonorListItem> found = readDonorsPort.search(text, search.type(), after, search.size() + 1);
        boolean hasMore = found.size() > search.size();
        List<DonorListItem> page = hasMore ? found.subList(0, search.size()) : found;
        return new DonorPage(page, hasMore ? DonorCursor.encode(page.getLast()) : null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DonorCount> countByType() {
        return readDonorsPort.countByType();
    }
}
