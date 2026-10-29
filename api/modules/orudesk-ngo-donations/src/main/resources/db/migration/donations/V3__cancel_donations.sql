-- Day 58: a donation is never deleted; it can be cancelled, with a date and a reason (a bounced cheque).
ALTER TABLE ngo_donation
    ADD COLUMN cancelled_on   DATE          NULL,
    ADD COLUMN cancel_reason  VARCHAR(200)  NULL,
    -- Both or neither: a cancellation always says when and why
    ADD CONSTRAINT ck_ngo_donation_cancellation CHECK ((cancelled_on IS NULL) = (cancel_reason IS NULL));

-- Totals and reports read only donations that stand. A partial index holds just those rows: smaller than a full
-- index, and the planner uses it for queries whose WHERE says "cancelled_on IS NULL" too.
CREATE INDEX ix_ngo_donation_standing ON ngo_donation (tenant_id, received_on) WHERE cancelled_on IS NULL;
