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
    private final DonorType type;
    private final Pan pan;                 // null: not given
    private final boolean wantsTaxReceipt;
    private final boolean receivesUpdates;
    private final LocalDate registeredOn;
    private final long version;

    private Donor(DonorId id, UUID personId, DonorType type, Pan pan, boolean wantsTaxReceipt,
                  boolean receivesUpdates, LocalDate registeredOn, long version) {
        this.id = Objects.requireNonNull(id);
        this.personId = Objects.requireNonNull(personId, "a donor is always a person");
        this.type = Objects.requireNonNull(type);
        this.pan = pan;
        this.wantsTaxReceipt = wantsTaxReceipt;
        this.receivesUpdates = receivesUpdates;
        this.registeredOn = Objects.requireNonNull(registeredOn);
        this.version = version;
    }

    /** @throws IllegalArgumentException if they want a tax receipt but gave no PAN: it's issued against one */
    public static Donor register(UUID personId, DonorType type, Pan pan, boolean wantsTaxReceipt,
                                 boolean receivesUpdates, LocalDate today) {
        requirePanForReceipt(pan, wantsTaxReceipt);
        return new Donor(DonorId.newId(), personId, type, pan, wantsTaxReceipt, receivesUpdates, today, 0);
    }

    /** For the persistence adapter: rebuilds a donor that already exists. */
    public static Donor restore(DonorId id, UUID personId, DonorType type, Pan pan, boolean wantsTaxReceipt,
                                boolean receivesUpdates, LocalDate registeredOn, long version) {
        return new Donor(id, personId, type, pan, wantsTaxReceipt, receivesUpdates, registeredOn, version);
    }

    /**
     * New donor details (Day 49), checked by the same rule as a registration: someone can't ask for tax receipts
     * after their PAN was taken off. The person and the registration date are facts, not details.
     *
     * @throws IllegalArgumentException if they want a tax receipt but would have no PAN
     */
    public Donor changeDetails(DonorType newType, Pan newPan, boolean newWantsTaxReceipt, boolean newReceivesUpdates) {
        requirePanForReceipt(newPan, newWantsTaxReceipt);
        return new Donor(id, personId, Objects.requireNonNull(newType), newPan, newWantsTaxReceipt, newReceivesUpdates,
                registeredOn, version);
    }

    private static void requirePanForReceipt(Pan pan, boolean wantsTaxReceipt) {
        if (wantsTaxReceipt && pan == null) {
            throw new IllegalArgumentException("a tax receipt needs a PAN");
        }
    }

    public DonorId id() { return id; }
    public UUID personId() { return personId; }
    public DonorType type() { return type; }
    public Pan pan() { return pan; }
    public boolean wantsTaxReceipt() { return wantsTaxReceipt; }
    public boolean receivesUpdates() { return receivesUpdates; }
    public LocalDate registeredOn() { return registeredOn; }
    public long version() { return version; }
}
