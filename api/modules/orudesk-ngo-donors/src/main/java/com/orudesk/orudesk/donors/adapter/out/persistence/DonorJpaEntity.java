package com.orudesk.orudesk.donors.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.time.LocalDate;
import java.util.UUID;

/** One row of ngo_donor. */
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

    @Column(length = 10)
    private String pan;

    @Column(name = "receives_updates", nullable = false)
    private boolean receivesUpdates;

    @Column(name = "registered_on", nullable = false)
    private LocalDate registeredOn;

    protected DonorJpaEntity() {
        // JPA needs a no-argument constructor
    }

    DonorJpaEntity(UUID id, UUID personId, String pan, boolean receivesUpdates, LocalDate registeredOn) {
        this.id = id;
        this.personId = personId;
        this.pan = pan;
        this.receivesUpdates = receivesUpdates;
        this.registeredOn = registeredOn;
    }

    UUID getId() { return id; }
    UUID getPersonId() { return personId; }
    String getPan() { return pan; }
    boolean isReceivesUpdates() { return receivesUpdates; }
    LocalDate getRegisteredOn() { return registeredOn; }
}
