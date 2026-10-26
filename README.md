# Orudesk NGO

**The Orudesk edition for NGOs**: everything in the [Orudesk core](https://github.com/orudesk/orudesk)
(login, roles, organisation units, the people registry) plus what NGOs need, starting with donors.

> Status: early. Expect breaking changes before 1.0.

## Quick start

You need **JDK 25** and **Docker**, and the core checked out next to this repository:

```bash
git clone https://github.com/orudesk/orudesk.git
git clone https://github.com/orudesk/orudesk-ngo.git
cd orudesk
cp api/.env.example api/.env                       # then edit the values
docker compose --env-file api/.env -f docker/compose.yaml up -d --wait
cp api/.env ../orudesk-ngo/api/.env
cd ../orudesk-ngo/api
./gradlew bootRun
```

Swagger UI: <http://localhost:8080/swagger-ui.html>. Log in, then try `registerDonor`.

## Run the tests

```bash
cd api
./gradlew test       # needs Docker
```

## Licence

GNU Affero General Public License v3.0 ([LICENSE](LICENSE)), like the core.
