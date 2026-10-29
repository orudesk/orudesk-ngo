package com.orudesk.orudesk.donations.domain;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Money a donor gave (Day 56). Immutable: a donation is a fact. A mistake is put right by cancelling it (Day 58)
 * and recording the right one, so the books always show what happened.
 */
public final class Donation {

    /**
     * Section 269ST of the Income Tax Act: nobody may receive two lakh rupees or more in cash in one transaction.
     * The NGO would pay a penalty equal to the amount. Refused at the door, and by a CHECK in the database.
     */
    public static final Amount CASH_LIMIT = Amount.rupees(200_000);

    private final DonationId id;
    private final UUID donorId;
    private final Amount amount;
    private final LocalDate receivedOn;
    private final PaymentMode mode;
    private final @Nullable String reference;
    private final @Nullable String purpose;
    private final long version;

    private Donation(DonationId id, UUID donorId, Amount amount, LocalDate receivedOn, PaymentMode mode,
                     @Nullable String reference, @Nullable String purpose, long version) {
        this.id = Objects.requireNonNull(id);
        this.donorId = Objects.requireNonNull(donorId, "a donation always has a donor");
        this.amount = Objects.requireNonNull(amount);
        this.receivedOn = Objects.requireNonNull(receivedOn);
        this.mode = Objects.requireNonNull(mode);
        this.reference = blankToNull(reference);
        this.purpose = blankToNull(purpose);
        this.version = version;
    }

    /**
     * @param today to refuse a date in the future: money can't have been received tomorrow
     * @throws IllegalArgumentException for a future date, cash of two lakh or more, or a missing reference
     */
    public static Donation record(UUID donorId, Amount amount, LocalDate receivedOn, PaymentMode mode,
                                  @Nullable String reference, @Nullable String purpose, LocalDate today) {
        if (receivedOn.isAfter(today)) {
            throw new IllegalArgumentException("a donation can't be received in the future");
        }
        if (mode == PaymentMode.CASH && amount.isAtLeast(CASH_LIMIT)) {
            throw new IllegalArgumentException("cash of " + CASH_LIMIT + " or more can't be accepted (section 269ST)");
        }
        if (mode.needsReference() && blankToNull(reference) == null) {
            throw new IllegalArgumentException("a " + mode.name().toLowerCase().replace('_', ' ')
                    + " payment needs its reference (cheque number, UTR or transaction id)");
        }
        return new Donation(DonationId.newId(), donorId, amount, receivedOn, mode, reference, purpose, 0);
    }

    /** For the persistence adapter: rebuilds a donation that already exists. */
    public static Donation restore(DonationId id, UUID donorId, Amount amount, LocalDate receivedOn, PaymentMode mode,
                                   @Nullable String reference, @Nullable String purpose, long version) {
        return new Donation(id, donorId, amount, receivedOn, mode, reference, purpose, version);
    }

    private static @Nullable String blankToNull(@Nullable String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }

    public DonationId id() { return id; }
    public UUID donorId() { return donorId; }
    public Amount amount() { return amount; }
    public LocalDate receivedOn() { return receivedOn; }
    public PaymentMode mode() { return mode; }
    public @Nullable String reference() { return reference; }
    public @Nullable String purpose() { return purpose; }
    public long version() { return version; }
}
