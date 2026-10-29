-- Day 56: money given to the NGO. One row per gift, never deleted (Day 58 cancels instead).
CREATE TABLE ngo_donation (
    id           UUID           NOT NULL PRIMARY KEY,
    tenant_id    UUID           NOT NULL,
    donor_id     UUID           NOT NULL,
    -- NUMERIC, not REAL or DOUBLE PRECISION: decimal digits, exact. (14, 2): up to 12 digits of rupees, 2 of paise.
    amount       NUMERIC(14, 2) NOT NULL,
    -- One currency today. The column makes the assumption visible, and foreign donations (FCRA) a later change.
    currency     CHAR(3)        NOT NULL DEFAULT 'INR',
    received_on  DATE           NOT NULL,
    mode         VARCHAR(20)    NOT NULL,
    reference    VARCHAR(50)    NULL,       -- cheque number, UTR, UPI transaction id
    purpose      VARCHAR(100)   NULL,       -- "General", "Education fund"...
    recorded_at  TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version      BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT fk_ngo_donation_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id),
    -- A donation can't outlive its donor's record: RESTRICT, so deleting a donor who gave fails loudly
    CONSTRAINT fk_ngo_donation_donor FOREIGN KEY (donor_id) REFERENCES ngo_donor (id) ON DELETE RESTRICT,
    CONSTRAINT ck_ngo_donation_amount CHECK (amount > 0),
    CONSTRAINT ck_ngo_donation_currency CHECK (currency = 'INR'),
    CONSTRAINT ck_ngo_donation_mode CHECK (mode IN ('CASH', 'CHEQUE', 'BANK_TRANSFER', 'UPI', 'CARD', 'ONLINE')),
    -- Section 269ST: no cash of two lakh rupees or more. The same rule as Donation.CASH_LIMIT, in the database too.
    CONSTRAINT ck_ngo_donation_cash_limit CHECK (mode <> 'CASH' OR amount < 200000),
    CONSTRAINT ck_ngo_donation_reference CHECK (mode = 'CASH' OR reference IS NOT NULL)
);

-- "This donor's donations, newest first" (the donor's page), and the FK's own lookups when a donor is deleted.
CREATE INDEX ix_ngo_donation_donor ON ngo_donation (tenant_id, donor_id, received_on DESC);
-- "Everything received between two dates" (reports, Day 59).
CREATE INDEX ix_ngo_donation_received ON ngo_donation (tenant_id, received_on);
