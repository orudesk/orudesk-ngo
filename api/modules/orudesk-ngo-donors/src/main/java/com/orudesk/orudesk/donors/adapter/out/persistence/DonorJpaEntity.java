package com.orudesk.orudesk.donors.adapter.out.persistence;

import com.orudesk.orudesk.donors.domain.DonorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.time.LocalDate;
import java.util.UUID;

/** One row of ngo_donor. Only ever written: reading goes through the summary view. */
@Entity
@Table(name = "ngo_donor")
class DonorJpaEntity {

    @Id
    private UUID id;

    @TenantId   // the core's tenant filter works for the verticals' entities too
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "person_id", nullable = false, updatable = false)
    private UUID personId;

    @Enumerated(EnumType.STRING)   // the name ('INDIVIDUAL'), never the position: adding a type can't renumber rows
    @Column(name = "donor_type", nullable = false, length = 20)
    private DonorType donorType;

    @Column(length = 10)
    private String pan;

    @Column(name = "wants_tax_receipt", nullable = false)
    private boolean wantsTaxReceipt;

    @Column(name = "receives_updates", nullable = false)
    private boolean receivesUpdates;

    @Column(name = "registered_on", nullable = false)
    private LocalDate registeredOn;

    protected DonorJpaEntity() {
        // JPA needs a no-argument constructor
    }

    DonorJpaEntity(UUID id, UUID personId, DonorType donorType, String pan, boolean wantsTaxReceipt,
                   boolean receivesUpdates, LocalDate registeredOn) {
        this.id = id;
        this.personId = personId;
        this.donorType = donorType;
        this.pan = pan;
        this.wantsTaxReceipt = wantsTaxReceipt;
        this.receivesUpdates = receivesUpdates;
        this.registeredOn = registeredOn;
    }
}
