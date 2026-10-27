-- Day 23: what kind of donor, and whether they want an 80G tax receipt.

-- PostgreSQL 11+ adds a column with a constant DEFAULT without rewriting the table: existing rows get
-- the default from the catalog, instantly, even with millions of donors.
ALTER TABLE ngo_donor
    ADD COLUMN donor_type        VARCHAR(20) NOT NULL DEFAULT 'INDIVIDUAL',
    ADD COLUMN wants_tax_receipt BOOLEAN     NOT NULL DEFAULT FALSE;

-- A CHECK on several columns: a tax receipt is issued against a PAN, so "wants one" without a PAN is
-- refused, whoever writes the row. Adding a CHECK reads every existing row once to verify it.
ALTER TABLE ngo_donor
    ADD CONSTRAINT ck_ngo_donor_type CHECK (donor_type IN ('INDIVIDUAL', 'CORPORATE', 'FOUNDATION')),
    ADD CONSTRAINT ck_ngo_donor_receipt_needs_pan CHECK (NOT wants_tax_receipt OR pan IS NOT NULL);
