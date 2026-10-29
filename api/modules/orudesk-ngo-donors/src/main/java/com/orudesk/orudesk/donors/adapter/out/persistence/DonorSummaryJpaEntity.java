package com.orudesk.orudesk.donors.adapter.out.persistence;

import com.orudesk.orudesk.donors.domain.DonorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TenantId;

import java.time.LocalDate;
import java.util.UUID;

/**
 * One row of the view v_ngo_donor_summary. To Hibernate a view looks like a table; @Immutable tells it
 * never to write one (no INSERT, no UPDATE, no dirty checking), and ddl-auto=validate checks its columns.
 */
@Entity
@Immutable
@Table(name = "v_ngo_donor_summary")
class DonorSummaryJpaEntity {

    @Id
    private UUID id;

    @TenantId   // the tenant filter works on a view exactly as on a table: it's a WHERE on tenant_id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "person_id")
    private UUID personId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private String phone;

    private String email;

    @Column(name = "search_text")
    private String searchText;

    @Enumerated(EnumType.STRING)
    @Column(name = "donor_type")
    private DonorType donorType;

    @Column(name = "pan_masked")
    private String panMasked;

    @Column(name = "wants_tax_receipt")
    private boolean wantsTaxReceipt;

    @Column(name = "receives_updates")
    private boolean receivesUpdates;

    @Column(name = "registered_on")
    private LocalDate registeredOn;

    private long version;

    @Column(name = "person_version")
    private long personVersion;

    protected DonorSummaryJpaEntity() {
        // JPA needs a no-argument constructor
    }

    UUID getId() { return id; }
    UUID getPersonId() { return personId; }
    String getFirstName() { return firstName; }
    String getLastName() { return lastName; }
    String getPhone() { return phone; }
    String getEmail() { return email; }
    DonorType getDonorType() { return donorType; }
    String getPanMasked() { return panMasked; }
    boolean isWantsTaxReceipt() { return wantsTaxReceipt; }
    boolean isReceivesUpdates() { return receivesUpdates; }
    LocalDate getRegisteredOn() { return registeredOn; }
    long getVersion() { return version; }
    long getPersonVersion() { return personVersion; }
}
