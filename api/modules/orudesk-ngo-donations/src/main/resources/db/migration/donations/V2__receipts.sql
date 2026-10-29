-- Day 57: every donation has a receipt, numbered without gaps per NGO and financial year ("2026-27/000042").

-- One row per NGO and year, holding the last number given out. The next number is taken with
--   INSERT ... ON CONFLICT DO UPDATE SET last_number = last_number + 1 RETURNING last_number
-- which locks this row until the donation's transaction ends: a second receipt in the same year waits, then gets
-- the next number; a transaction that rolls back gives its number back. A SEQUENCE can't do either: it never
-- waits, never gives a number back (rollbacks leave gaps), and isn't per tenant or per year.
CREATE TABLE ngo_receipt_counter (
    tenant_id       UUID        NOT NULL,
    financial_year  VARCHAR(7)  NOT NULL,           -- '2026-27'
    last_number     INTEGER     NOT NULL,
    PRIMARY KEY (tenant_id, financial_year),
    CONSTRAINT fk_ngo_receipt_counter_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id),
    CONSTRAINT ck_ngo_receipt_counter_positive CHECK (last_number > 0)
);

ALTER TABLE ngo_donation
    ADD COLUMN financial_year    VARCHAR(7)  NULL,
    ADD COLUMN receipt_sequence  INTEGER     NULL,
    ADD COLUMN receipt_issued_on DATE        NULL;

-- Donations recorded before today (Day 56) get their receipts now: numbered in the order they were received,
-- year by year. A window function numbers each year's rows in one pass, no loop needed.
WITH numbered AS (
    SELECT id,
           CASE WHEN EXTRACT(MONTH FROM received_on) >= 4
                THEN EXTRACT(YEAR FROM received_on)::int
                ELSE EXTRACT(YEAR FROM received_on)::int - 1 END AS start_year,
           recorded_at
    FROM ngo_donation
), labelled AS (
    SELECT id, start_year::text || '-' || lpad(((start_year + 1) % 100)::text, 2, '0') AS financial_year, recorded_at
    FROM numbered
)
UPDATE ngo_donation d
SET financial_year    = l.financial_year,
    receipt_sequence  = l.seq,
    receipt_issued_on = d.recorded_at::date
FROM (
    SELECT l.id, l.financial_year,
           ROW_NUMBER() OVER (PARTITION BY dn.tenant_id, l.financial_year ORDER BY dn.received_on, dn.recorded_at, dn.id) AS seq
    FROM labelled l JOIN ngo_donation dn ON dn.id = l.id
) l
WHERE d.id = l.id;

-- ...and the counters continue from there
INSERT INTO ngo_receipt_counter (tenant_id, financial_year, last_number)
SELECT tenant_id, financial_year, MAX(receipt_sequence) FROM ngo_donation GROUP BY tenant_id, financial_year;

ALTER TABLE ngo_donation
    ALTER COLUMN financial_year SET NOT NULL,
    ALTER COLUMN receipt_sequence SET NOT NULL,
    ALTER COLUMN receipt_issued_on SET NOT NULL,
    -- A number is used once per NGO and year: the database's guarantee, whatever the code does
    ADD CONSTRAINT uk_ngo_donation_receipt UNIQUE (tenant_id, financial_year, receipt_sequence),
    ADD CONSTRAINT ck_ngo_donation_receipt_sequence CHECK (receipt_sequence BETWEEN 1 AND 999999),
    ADD CONSTRAINT ck_ngo_donation_financial_year CHECK (financial_year ~ '^[0-9]{4}-[0-9]{2}$');
