-- Day 49: a donor's details (type, PAN, tax receipts, updates) can be edited, with optimistic locking like the
-- core's people (party V3): an edit names the version it was made on.
ALTER TABLE ngo_donor ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
