-- A donor with their name and contact details from the People registry, ready to list, search and report on.
--
-- R__ ("repeatable"): Flyway runs this file again whenever its content changes, after the module's
-- V migrations. That suits a view: it holds no data, so the whole definition is simply replaced.
--
-- A security view too: it shows the PAN only masked. A reporting user can be given SELECT on this view
-- and never on ngo_donor itself (see the Day 23 plan, Part 8).
--
-- CREATE OR REPLACE VIEW can only add columns at the end, never rename or reorder them: new columns go last.
CREATE OR REPLACE VIEW v_ngo_donor_summary AS
SELECT d.id,
       d.tenant_id,
       d.person_id,
       p.first_name,
       p.last_name,
       p.phone,
       p.email,
       p.search_text,
       d.donor_type,
       'XXXXXX' || right(d.pan, 4) AS pan_masked,      -- NULL when there's no PAN: NULL || text is NULL
       d.wants_tax_receipt,
       d.receives_updates,
       d.registered_on,
       d.version,                                     -- Day 49: an edit sends back both versions it read,
       p.version AS person_version                    -- so the form gets them in one read
FROM ngo_donor d
         -- the tenant in the join as well: a donor can never be shown with another tenant's person
         JOIN party_person p ON p.id = d.person_id AND p.tenant_id = d.tenant_id;
