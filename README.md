# Orudesk NGO

**The Orudesk edition for NGOs**: everything in the [Orudesk core](https://github.com/orudesk/orudesk)
(login, roles, organisation units, the people registry) plus what NGOs need, starting with donors.

> Status: release 1 (1.0.0), built on Orudesk core 1.0.0.

## Quick start: run it

You need only **Docker**, and the core checked out next to this repository (the image is built from both):

```bash
git clone https://github.com/orudesk/orudesk.git
git clone https://github.com/orudesk/orudesk-ngo.git
cp orudesk/api/.env.example orudesk-ngo/api/.env       # then set DB_PASSWORD, ORUDESK_JWT_SECRET and the first admin
docker compose --env-file orudesk-ngo/api/.env -f orudesk-ngo/docker/compose.yaml up -d --build --wait
```

Swagger UI: <http://localhost:8081/swagger-ui.html>. Log in, then try `registerDonor`.

## Quick start: develop it

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
