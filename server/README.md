# ShelfTap server

Spring Boot 4.1 (Java 21) REST API on PostgreSQL. The wire contract is [`API.md`](API.md); the product rules are `SPEC.md` §6. The security mechanisms are listed in `security/README.md`.

## Run it (dev)

All commands run from `server/`, with Docker and JDK 21 installed.

```bash
# 1. Secrets: copy the template and fill every value (each with `openssl rand -base64 32`). server/.env is gitignored.
cp .env.example .env
set -a; . ./.env; set +a

# 2. Dev TLS keystore (self-signed, SAN localhost, 127.0.0.1, 10.0.2.2; pass a LAN IP to add it)
scripts/dev-tls.sh            # writes dev-tls/shelftap-dev.p12 and dev-tls/shelftap-dev.crt (gitignored)

# 3. PostgreSQL
docker compose --env-file .env up -d

# 4. Server on https://localhost:8443 (Flyway migrates, the dev profile seeds the demo catalog and two accounts)
../gradlew :server:bootRun --args='--spring.profiles.active=dev'
curl --cacert dev-tls/shelftap-dev.crt 'https://localhost:8443/api/titles?size=2'
```

From the Android emulator the server is `https://10.0.2.2:8443`. Clients must trust `dev-tls/shelftap-dev.crt`.

## Settings

Everything is an env var or a `shelftap.*` property (`config/ShelfTapProperties.java`). There are no defaults for secrets, and the server **refuses to start** when one is missing, not Base64, or under 32 bytes.

| Env var | What |
|---|---|
| `SHELFTAP_JWT_KEY` | HS256 key that signs access tokens |
| `SHELFTAP_MASTER_KEY` | `K_master`: derives each card's admin key (`card-spec.md` §2) |
| `SHELFTAP_GATE_KEY` | Shared secret of the gate, sent as `X-Gate-Key` |
| `SHELFTAP_TLS_KEYSTORE`, `SHELFTAP_TLS_KEYSTORE_PASSWORD` | PKCS12 keystore for the only listener, 8443, TLS 1.2+ |
| `SHELFTAP_DB_URL`, `SHELFTAP_DB_USER`, `SHELFTAP_DB_PASSWORD` | PostgreSQL |
| `SHELFTAP_DEV_LIBRARIAN_PASSWORD`, `SHELFTAP_DEV_MEMBER_PASSWORD` | Profile `dev` only: demo account passwords |

Policy (`shelftap.loan.*`): `days` 14, `max-loans` 5, `max-renewals` 1, `fine-per-day-vnd` 1000, `reservation-hold` P3D. Auth (`shelftap.auth.*`): `max-failed-logins` 5, `lockout-window` PT15M. `shelftap.rate-limit.*`: 20 requests per minute per IP. `shelftap.privacy.history-retention`: P365D.

## Dev data (profile `dev`)

- Librarian: username `librarian`. Member: `member@shelftap.test`. Passwords are the two `SHELFTAP_DEV_*` values.
- 13 titles (Vietnamese and English) with 24 copies. Copy *n* (1 to 24) has tag ID `0000000000` + two-hex *n* + `04E0` (copy 1 is `00000000000104E0`) and barcode `SHELF-000n`. Copy 6 of "Chí Phèo" is withdrawn.

## Tests

`./gradlew :server:test` needs Docker: one PostgreSQL 17 Testcontainer serves the whole run, and keys are random per run. The card login test issues the real applet in jCardSim (in its own class loader, because jCardSim bundles an old BouncyCastle).

## Not done yet

- **Staff accounts** have no API. Insert a row into `staff` with an Argon2id hash (dev profile creates one librarian). Admin screens come with `desktop/`.
- **Check-in, fine payment, catalog editing, C4 admin-response** have no route (`API.md`). `LoanService.checkIn` (fine accrual, hand the copy to the next reservation) is built and tested, ready for the desktop route.
- The rate limiter is in memory, so it holds for one server instance. Behind a proxy, every client would share the proxy's IP.
