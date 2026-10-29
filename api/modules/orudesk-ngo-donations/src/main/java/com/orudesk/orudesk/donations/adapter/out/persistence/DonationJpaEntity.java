package com.orudesk.orudesk.donations.adapter.out.persistence;

import com.orudesk.orudesk.donations.domain.PaymentMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "ngo_donation")
class DonationJpaEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "donor_id", nullable = false, updatable = false)
    private UUID donorId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "received_on", nullable = false)
    private LocalDate receivedOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMode mode;

    @Column(length = 50)
    private @Nullable String reference;

    @Column(length = 100)
    private @Nullable String purpose;

    @Column(name = "financial_year", nullable = false, updatable = false, length = 7)
    private String financialYear;

    @Column(name = "receipt_sequence", nullable = false, updatable = false)
    private int receiptSequence;

    @Column(name = "receipt_issued_on", nullable = false, updatable = false)
    private LocalDate receiptIssuedOn;

    @Version
    private Long version;

    protected DonationJpaEntity() {
        // JPA needs a no-argument constructor
    }

    DonationJpaEntity(UUID id, UUID donorId, BigDecimal amount, LocalDate receivedOn, PaymentMode mode,
                      @Nullable String reference, @Nullable String purpose, String financialYear, int receiptSequence,
                      LocalDate receiptIssuedOn) {
        this.id = id;
        this.donorId = donorId;
        this.amount = amount;
        this.receivedOn = receivedOn;
        this.mode = mode;
        this.reference = reference;
        this.purpose = purpose;
        this.financialYear = financialYear;
        this.receiptSequence = receiptSequence;
        this.receiptIssuedOn = receiptIssuedOn;
    }

    UUID getId() { return id; }
    UUID getDonorId() { return donorId; }
    BigDecimal getAmount() { return amount; }
    LocalDate getReceivedOn() { return receivedOn; }
    PaymentMode getMode() { return mode; }
    @Nullable String getReference() { return reference; }
    @Nullable String getPurpose() { return purpose; }
    String getFinancialYear() { return financialYear; }
    int getReceiptSequence() { return receiptSequence; }
    LocalDate getReceiptIssuedOn() { return receiptIssuedOn; }
    long getVersion() { return version == null ? 0 : version; }
}
