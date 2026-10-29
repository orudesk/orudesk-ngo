package com.orudesk.orudesk.donations.adapter.out.persistence;

import com.orudesk.orudesk.donations.application.port.out.LoadDonationPort;
import com.orudesk.orudesk.donations.application.port.out.SaveDonationPort;
import com.orudesk.orudesk.donations.domain.Amount;
import com.orudesk.orudesk.donations.domain.Donation;
import com.orudesk.orudesk.donations.domain.DonationId;
import com.orudesk.orudesk.donations.domain.FinancialYear;
import com.orudesk.orudesk.donations.domain.ReceiptNumber;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
class DonationPersistenceAdapter implements SaveDonationPort, LoadDonationPort {

    private final DonationJpaRepository donations;

    DonationPersistenceAdapter(DonationJpaRepository donations) {
        this.donations = donations;
    }

    @Override
    public void save(Donation donation) {
        ReceiptNumber receipt = Objects.requireNonNull(donation.receiptNumber(), "a donation is saved with its receipt");
        donations.saveAndFlush(new DonationJpaEntity(donation.id().value(), donation.donorId(),
                donation.amount().value(), donation.receivedOn(), donation.mode(), donation.reference(),
                donation.purpose(), receipt.year().label(), receipt.sequence(),
                Objects.requireNonNull(donation.receiptIssuedOn())));
    }

    @Override
    public Optional<Donation> findById(DonationId id) {
        return donations.findById(id.value()).map(DonationPersistenceAdapter::toDomain);
    }

    @Override
    public List<Donation> findByDonor(UUID donorId) {
        return donations.findByDonorIdOrderByReceivedOnDescIdDesc(donorId).stream()
                .map(DonationPersistenceAdapter::toDomain).toList();
    }

    @Override
    public Optional<Amount> totalOf(UUID donorId) {
        BigDecimal sum = donations.sumByDonor(donorId);
        return sum == null ? Optional.empty() : Optional.of(new Amount(sum));
    }

    private static Donation toDomain(DonationJpaEntity row) {
        return Donation.restore(new DonationId(row.getId()), row.getDonorId(), new Amount(row.getAmount()),
                row.getReceivedOn(), row.getMode(), row.getReference(), row.getPurpose(),
                new ReceiptNumber(FinancialYear.parse(row.getFinancialYear()), row.getReceiptSequence()),
                row.getReceiptIssuedOn(), row.getVersion());
    }
}
