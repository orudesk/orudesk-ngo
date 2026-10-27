# Changelog

Every release of Orudesk NGO, newest first ([Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
[Semantic Versioning](https://semver.org/spec/v2.0.0.html)). Each release names the core release it is built on.

## [Unreleased]

### Added

- The core's audit log: the app includes `orudesk-audit`, so registering people and users and giving roles are
  recorded, and admins have the Activity page.

- `web/`: the NGO edition's web app, the core's screens plus Donors: counts by type, search and filter by
  type, add a donor (the server's messages under each field), and a donor's page with the PAN masked.
- The edition's OpenAPI contract, saved by `OpenApiContractTest` to `build/openapi/orudesk-ngo.json`; CI
  checks the web app's copy is up to date.

### Changed

- The contract says 201 Created for `POST /api/v1/donors`, and is sorted and indented.
- The database pool waits at most 5 s for a connection (fail fast with a 503 when the database is down).

## [1.0.0] - 2026-10-26

Release 1, built on Orudesk core 1.0.0.

### Added

- Donors: register a donor (a person plus PAN, donor type and tax-receipt consent), search donors, counts by type, and a summary view for reports.
- Docker image `ghcr.io/orudesk/orudesk-ngo`, and `docker/compose.yaml` to run the edition with its database.
