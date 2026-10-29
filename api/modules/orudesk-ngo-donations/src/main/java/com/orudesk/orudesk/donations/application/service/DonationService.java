package com.orudesk.orudesk.donations.application.service;

import com.orudesk.orudesk.donations.application.port.in.CancelDonationCommand;
import com.orudesk.orudesk.donations.application.port.in.CancelDonationUseCase;
import com.orudesk.orudesk.donations.application.port.in.DonationCancelled;
import com.orudesk.orudesk.donations.application.port.in.DonationDetails;
import com.orudesk.orudesk.donations.application.port.in.DonationReceipt;
import com.orudesk.orudesk.donations.application.port.in.DonationRecorded;
import com.orudesk.orudesk.donations.application.port.in.DonorDonations;
import com.orudesk.orudesk.donations.application.port.in.GetDonationQuery;
import com.orudesk.orudesk.donations.application.port.in.RecordDonationCommand;
import com.orudesk.orudesk.donations.application.port.in.RecordDonationUseCase;
import com.orudesk.orudesk.donations.application.port.out.LoadDonationPort;
import com.orudesk.orudesk.donations.application.port.out.OrganisationDetails;
import com.orudesk.orudesk.donations.application.port.out.ReceiptNumbers;
import com.orudesk.orudesk.donations.application.port.out.SaveDonationPort;
import com.orudesk.orudesk.donations.domain.Amount;
import com.orudesk.orudesk.donations.domain.Cancellation;
import com.orudesk.orudesk.donations.domain.Donation;
import com.orudesk.orudesk.donations.domain.DonationId;
import com.orudesk.orudesk.donations.domain.DonationNotFoundException;
import com.orudesk.orudesk.donations.domain.PaymentMode;
import com.orudesk.orudesk.donations.domain.ReceiptNumber;
import com.orudesk.orudesk.donations.domain.RupeesInWords;
import com.orudesk.orudesk.donations.domain.TaxDeduction;
import com.orudesk.orudesk.donations.domain.UnknownDonorException;
import com.orudesk.orudesk.donors.application.port.in.DonorForReceipt;
import com.orudesk.orudesk.donors.application.port.in.DonorReceiptQuery;
import com.orudesk.orudesk.shared.EditConflictException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
class DonationService implements RecordDonationUseCase, GetDonationQuery, CancelDonationUseCase {

    private final DonorReceiptQuery donors;
    private final SaveDonationPort saveDonationPort;
    private final LoadDonationPort loadDonationPort;
    private final ReceiptNumbers receiptNumbers;
    private final OrganisationDetails organisation;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    DonationService(DonorReceiptQuery donors, SaveDonationPort saveDonationPort, LoadDonationPort loadDonationPort,
                    ReceiptNumbers receiptNumbers, OrganisationDetails organisation, Clock clock,
                    ApplicationEventPublisher events) {
        this.donors = donors;
        this.saveDonationPort = saveDonationPort;
        this.loadDonationPort = loadDonationPort;
        this.receiptNumbers = receiptNumbers;
        this.organisation = organisation;
        this.clock = clock;
        this.events = events;
    }

    /**
     * The donor is looked up through the donors module's API, in this tenant: a donation for another NGO's donor
     * is refused like a donor that doesn't exist. The table's foreign key backs that up.
     */
    @Override
    @Transactional
    public UUID record(RecordDonationCommand command) {
        DonorForReceipt donor = donors.findForReceipt(command.donorId())
                .orElseThrow(() -> new UnknownDonorException(command.donorId()));
        LocalDate today = LocalDate.now(clock);
        Donation recorded = Donation.record(donor.donorId(), Amount.of(command.amount()), command.receivedOn(),
                mode(command.mode()), command.reference(), command.purpose(), today);
        // Every donation gets its receipt at once, numbered in the series of the year the money arrived in. The
        // number is taken in this transaction: if saving fails, it's given back and the series has no gap.
        Donation donation = recorded.issueReceipt(receiptNumbers.next(recorded.financialYear()), today);
        saveDonationPort.save(donation);
        events.publishEvent(new DonationRecorded(donation.id().value(), donor.donorId(), donor.fullName(),
                donation.amount().formatted()));
        return donation.id().value();
    }

    @Override
    @Transactional
    public DonationDetails cancel(CancelDonationCommand command) {
        DonationId id = new DonationId(command.donationId());
        Donation current = loadDonationPort.findById(id).orElseThrow(() -> new DonationNotFoundException(id));
        if (current.version() != command.version()) {
            throw new EditConflictException("donation", id.value(), command.version(), current.version());
        }
        Donation cancelled = current.cancel(new Cancellation(LocalDate.now(clock), command.reason()));
        saveDonationPort.update(cancelled);
        events.publishEvent(new DonationCancelled(id.value(), Objects.requireNonNull(cancelled.receiptNumber()).value(),
                cancelled.amount().formatted(), Objects.requireNonNull(cancelled.cancellation()).reason()));
        return getDonation(id.value());
    }

    @Override
    @Transactional(readOnly = true)
    public DonationDetails getDonation(UUID donationId) {
        DonationId id = new DonationId(donationId);
        Donation donation = loadDonationPort.findById(id).orElseThrow(() -> new DonationNotFoundException(id));
        String donorName = donors.findForReceipt(donation.donorId()).map(DonorForReceipt::fullName).orElse("");
        return details(donation, donorName);
    }

    @Override
    @Transactional(readOnly = true)
    public DonationReceipt receiptFor(UUID donationId) {
        DonationId id = new DonationId(donationId);
        Donation donation = loadDonationPort.findById(id).orElseThrow(() -> new DonationNotFoundException(id));
        DonorForReceipt donor = donors.findForReceipt(donation.donorId())
                .orElseThrow(() -> new IllegalStateException("donation " + donationId + " has no donor"));
        TaxDeduction deduction = TaxDeduction.decide(donation, donor.wantsTaxReceipt(), donor.pan(),
                organisation.registration80G());
        ReceiptNumber number = Objects.requireNonNull(donation.receiptNumber());
        return new DonationReceipt(donation.id().value(), number.value(), number.year().label(),
                Objects.requireNonNull(donation.receiptIssuedOn()), organisation.name(), organisation.address(),
                organisation.pan(), organisation.registration80G(), donor.fullName(),
                deduction.eligible() ? donor.pan() : null,
                donation.amount().plain(), donation.amount().formatted(), RupeesInWords.of(donation.amount()),
                donation.receivedOn(), donation.mode().name(), donation.reference(), donation.purpose(),
                deduction.eligible(), deduction.reason(),
                donation.cancellation() == null ? null : donation.cancellation().on(),
                donation.cancellation() == null ? null : donation.cancellation().reason());
    }

    @Override
    @Transactional(readOnly = true)
    public DonorDonations donationsOf(UUID donorId) {
        DonorForReceipt donor = donors.findForReceipt(donorId).orElseThrow(() -> new UnknownDonorException(donorId));
        Amount total = loadDonationPort.totalOf(donorId).orElse(null);
        return new DonorDonations(
                loadDonationPort.findByDonor(donorId).stream().map(d -> details(d, donor.fullName())).toList(),
                total == null ? "0.00" : total.plain(),
                total == null ? "₹0.00" : total.formatted());
    }

    private static PaymentMode mode(String mode) {
        try {
            return PaymentMode.valueOf(mode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown payment mode: " + mode);
        }
    }

    private static DonationDetails details(Donation donation, String donorName) {
        return new DonationDetails(donation.id().value(), donation.donorId(), donorName, donation.amount().plain(),
                donation.amount().formatted(), donation.receivedOn(), donation.mode().name(), donation.reference(),
                donation.purpose(), Objects.requireNonNull(donation.receiptNumber()).value(),
                donation.cancellation() == null ? null : donation.cancellation().on(),
                donation.cancellation() == null ? null : donation.cancellation().reason(), donation.version());
    }
}
