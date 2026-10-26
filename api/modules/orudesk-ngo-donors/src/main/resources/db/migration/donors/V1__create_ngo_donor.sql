-- A donor is a person from the People registry (party_person) plus what only an NGO needs.
-- The ngo_ prefix: every table of the NGO edition, so they never clash with the core's or another vertical's.
CREATE TABLE ngo_donor (
    id                UUID          NOT NULL,
    tenant_id         UUID          NOT NULL,
    person_id         UUID          NOT NULL,
    pan               VARCHAR(10)   NULL,
    receives_updates  BOOLEAN       NOT NULL DEFAULT FALSE,
    registered_on     DATE          NOT NULL,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_ngo_donor_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id),
    -- RESTRICT: a person who is a donor can't be deleted from the registry by accident
    CONSTRAINT fk_ngo_donor_person FOREIGN KEY (person_id) REFERENCES party_person (id) ON DELETE RESTRICT,
    -- One donor record per person
    CONSTRAINT uk_ngo_donor_person UNIQUE (tenant_id, person_id),
    -- One donor per PAN in a tenant. NULLs are distinct by default, so any number of donors may have no PAN.
    CONSTRAINT uk_ngo_donor_pan UNIQUE (tenant_id, pan),
    -- The same rule as the Pan class: the database refuses bad data even if it doesn't come through Java
    CONSTRAINT ck_ngo_donor_pan CHECK (pan ~ '^[A-Z]{5}[0-9]{4}[A-Z]$')
);
