package com.orudesk.orudesk.donors.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A donor: a person from the People registry (personId), plus what the NGO needs about them as a donor.
 * The name, phone and email stay in the registry, so they're never copied here and never go out of date.
 */
public final class Donor {

    private final DonorId id;
    private final UUID personId;
    private final Pan pan;                 // null: not given (they won't get a tax receipt)
    private final boolean receivesUpdates;
    private final LocalDate registeredOn;

    private Donor(DonorId id, UUID personId, Pan pan, boolean receivesUpdates, LocalDate registeredOn) {
        this.id = Objects.requireNonNull(id);
        this.personId = Objects.requireNonNull(personId, "a donor is always a person");
        this.pan = pan;
        this.receivesUpdates = receivesUpdates;
        this.registeredOn = Objects.requireNonNull(registeredOn);
    }

    public static Donor register(UUID personId, Pan pan, boolean receivesUpdates, LocalDate today) {
        return new Donor(DonorId.newId(), personId, pan, receivesUpdates, today);
    }

    /** For the persistence adapter: rebuilds a donor that already exists. */
    public static Donor restore(DonorId id, UUID personId, Pan pan, boolean receivesUpdates, LocalDate registeredOn) {
        return new Donor(id, personId, pan, receivesUpdates, registeredOn);
    }

    public DonorId id() { return id; }
    public UUID personId() { return personId; }
    public Pan pan() { return pan; }
    public boolean receivesUpdates() { return receivesUpdates; }
    public LocalDate registeredOn() { return registeredOn; }
}
