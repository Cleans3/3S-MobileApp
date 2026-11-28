# Mobile app security: source of truth

Everything about protecting ShelfTap lives here: threat model, docs, key management, privacy, hardening notes, findings.

## Group links

- Group sheet (function description and security mechanisms): https://docs.google.com/spreadsheets/d/158fRTBiC3M0qO0SwRn_-uhCzyg1NfNPU/edit?usp=sharing&ouid=116178545089263203221&rtpof=true&sd=true

## Scope

- Secure storage (Keystore/Keychain), transport security, platform hardening, anti-tampering.
- Key rule: the member app never holds a tag password and never decides whether a book may leave. It only requests a loan; the server records it, and the gate or staff kiosk checks loan status with the server.
- Reading history is sensitive personal data (Decree 13/2023).

## Functions and their security mechanisms

One row per function, ready to copy into the group sheet. The two columns match the sheet's headers. Where each mechanism lives in code is listed under [Where each mechanism lives](#where-each-mechanism-lives).

| Mô tả chức năng | Các cơ chế bảo mật tích hợp |
|---|---|
| **Password login (Android).** The member logs in with email and password and gets a session. | Password hashed with Argon2id on the server, never stored in clear. The same error is returned for a wrong password and an unknown email, so accounts cannot be enumerated. The account locks for 15 minutes after 5 failed tries, and each IP is rate-limited (HTTP 429 with `Retry-After`). Traffic is HTTPS only (TLS 1.2+), with cleartext disabled in the app. |
| **Session and tokens.** The app stays logged in without asking for the password again. | Short-lived JWT access token (15 min), kept only in memory. Opaque 256-bit refresh token, stored on the server only as a SHA-256 hash, rotated on every use and bound to the device ID. Reusing an old refresh token revokes the whole token family (theft detection). On the phone the refresh token is encrypted with AES-256-GCM using a non-exportable Android Keystore key (StrongBox when the phone has it). An optional biometric lock (BiometricPrompt) guards the key. The app logs out automatically after a period of inactivity. |
| **Card login over NFC (Android).** The member taps the library card to the phone to log in. | Challenge-response with RSA-2048: the server issues a random 32-byte nonce that is single use, expires after 60 s and is bound to the card ID. The card signs `cardId ‖ nonce` with a private key that never leaves the chip (RSASSA-PKCS1-v1_5, SHA-256), and the server verifies it with the public key registered at issue. The server rejects blocked, lost or revoked cards. No PIN and no personal data cross NFC, because the applet refuses those commands over contactless (`6986`). The token records `amr=card` (possession only), and challenge requests are rate-limited. |
| **First login and password change.** Staff create the account, and the member must set a new password. | Members cannot self-register; staff create the account after checking the member's ID. Every account starts with `mustChangePassword`, and until the password changes, the server answers 403 to everything else. The current password is required to change it, there is a minimum length, and the change revokes every other session. |
| **Catalog search and title details.** Anyone, even a guest, searches books and sees availability. | Read-only public endpoints. Parameterised queries (JPA) prevent SQL injection, and input is validated with a page-size cap. Errors are uniform `problem+json` responses without stack traces. IDs are random UUIDs, not sequential numbers. |
| **Self-checkout by tapping a book tag.** The member taps the book's NFC tag, and the server records the loan. | The app only reads the tag UID and sends a loan request. It holds no tag password, never writes the tag and never decides. The server checks the whole policy (copy available, loan limit, unpaid fines, reservations) inside one transaction with a row lock, so two taps cannot both borrow the same copy. Every loan is written to the audit log with the device ID and device-integrity flags. |
| **My loans, renewals and fines.** The member sees current loans and fines and renews a loan. | Role-based access (`MEMBER`). Ownership is checked on every object, and someone else's loan answers 404 rather than 403, so IDs cannot be probed (IDOR). The server decides renewals from policy (one renewal, none if overdue or reserved by someone else). |
| **Reservations.** The member reserves a title or cancels a reservation. | Ownership checks as above. Duplicate reservations are rejected, and actions are audit-logged. |
| **Reading history and privacy setting.** The member sees past loans and can turn history off. | Treated as sensitive personal data under Decree 13/2023. A card-login session (card possession only) cannot read history or change the setting; the server answers `STEP_UP_REQUIRED` until the member logs in with a password. The member can turn history off, and a scheduled job removes the member link from returned loans after a retention period. Screens with personal data set `FLAG_SECURE` (no screenshots, hidden in the recent-apps view), and nothing personal is written to logs. |
| **Due-date reminders (Android).** The phone reminds the member the day before a loan falls due. | Worked out on the phone from the loan list the server already returns; no server sends anything and no third-party push service is used, so nothing about the member's loans leaves the app. Off until the member turns it on; on Android 13 and up the notification permission is asked only after a short explanation, and a refusal leaves the feature off. "Hide book titles on the lock screen" is on by default: the notification is private (the full text shows only after unlock) and carries a public version that says only that a book is due. The alarms go to a receiver that is not exported, so no other app can trigger or read them. They are inexact alarms, so the app needs no exact-alarm permission. They are cancelled when the member turns the setting off, when Android stops allowing notifications, and on sign-out, token reuse or a lock key that no longer works. The app stores only which days have an alarm, not titles; a title is held in the alarm's own extras and only for a day with a single loan. |
| **Gate check (simulated gate).** The gate asks whether a tagged book is on loan. | A device key in the `X-Gate-Key` header, compared in constant time and read from an environment variable. The answer is a yes/no only, with no member data. |
| **Staff: register member, issue card, mark card lost/revoked.** Used by the desktop app. | `LIBRARIAN`/`ADMIN` roles with separate staff login. The card admin key is derived on the server as `HMAC-SHA256(K_master, "ShelfTap-admin" ‖ cardId)`, and `K_master` exists only in the server environment. A card's public key is write-once (`KEY_ALREADY_REGISTERED`), so staff cannot swap in their own key to impersonate a member's card. Issuing a new card revokes the member's old cards. A lost card is revoked on the server, which also ends every session started from it. Every card action is audit-logged. |
| **App protection (whole Android app).** Hardening that covers every screen. | Backups and device transfer disabled (`allowBackup=false`, data-extraction rules exclude everything). Only the launcher activity is exported. NFC is read only while the app is in the foreground. The release build is shrunk and obfuscated with R8, and log calls are stripped. Release builds pin the server certificate (OkHttp `CertificatePinner`). Root, emulator, debugger and signature-tamper checks run and are reported to the server in `X-Device-Integrity`; the app warns but never decides on them. |
| **Server platform (backs every function).** | HTTPS only on port 8443 (TLS 1.2+). Security headers: HSTS, `nosniff`, frame deny, `Content-Security-Policy: default-src 'none'`. CORS is off, and CSRF protection is off only because the API is stateless with bearer tokens. Actuator exposes health only. Secrets (JWT key, `K_master`, gate key, DB password) come from environment variables, and the server refuses to start without them. No secret is in this public repo. Every auth, card and circulation action is written to the audit log (who, what, when, IP, device). |

## Where each mechanism lives

`S/` = `server/src/main/java/shelftap/server/`, `A/` = `android/app/src/main/kotlin/shelftap/android/`, `M/` = `android/app/src/main/`. The tests that prove each server mechanism are in `server/src/test/java/shelftap/server/`, and the Android ones in `android/app/src/test/`.

| Mechanism | Server | Android |
|---|---|---|
| Password hashing (Argon2id), no user enumeration | `S/security/Passwords.java`, `S/auth/AuthService.java` | – |
| Lockout and rate limiting | `S/auth/LockoutService.java`, `S/web/RateLimiter.java`, `S/web/RateLimitFilter.java` | – |
| JWT access token, `amr` claim | `S/security/JwtService.java` | memory only: `A/session/SessionManager.kt` |
| Refresh token rotation, reuse detection, device binding | `S/auth/TokenService.java`, `S/auth/AuthService.java`, `S/auth/RefreshTokenRepository.java` | single refresh and retry, wipe on `TOKEN_REUSED`: `A/api/Net.kt`, `A/session/SessionManager.kt` |
| Refresh token encrypted at rest (Keystore AES-256-GCM, StrongBox) | stored only as SHA-256 | `A/security/KeystoreRefreshTokenVault.kt`, `A/security/AesGcm.kt` |
| Biometric lock | – | `A/security/BiometricLock.kt`, `A/security/KeystoreRefreshTokenVault.kt` |
| Inactivity logout | – | `A/session/InactivityTracker.kt` |
| Per-install device ID | – | `A/data/DeviceIdStore.kt` |
| `mustChangePassword` gate, password change | `S/security/PasswordChangeGateFilter.java`, `S/auth/AuthService.java` | password kept out of saved state: `A/ui/AuthScreens.kt` |
| Card login (C7 challenge-response) | `S/card/CardService.java`, `S/card/CardCrypto.java`, `S/card/CardChallengeRepository.java` | `A/nfc/Apdu.kt`, `A/session/SessionManager.kt` |
| Step-up for history and privacy | see `server/README.md` | `A/session/SessionManager.kt`, `A/ui/AuthScreens.kt` (`StepUpDialog`) |
| Card admin key derivation, write-once public key, revocation | `S/card/CardCrypto.java`, `S/card/CardService.java` | – |
| RBAC, deny by default, IDOR protection | `S/security/SecurityConfig.java`, `S/loan/LoanService.java`, `S/loan/ReservationService.java` | – |
| Self-checkout: server decides, row locks | `S/loan/LoanService.java`, `S/catalog/CopyRepository.java`, unique index in `server/src/main/resources/db/migration/V1__schema.sql` | tag ID only, no tag write: `A/nfc/TagId.kt`, `A/nfc/TagReader.kt`, `A/data/Repository.kt` |
| Gate key (constant time) | `S/loan/GateController.java`, `S/security/Hashing.java` | – |
| Audit log (append-only, DB trigger) | `S/audit/AuditService.java`, trigger in `V1__schema.sql` | integrity flags sent: `A/security/IntegrityChecker.kt`, `A/security/Integrity.kt` |
| Reading history opt-out, retention unlinking (Decree 13/2023) | `S/member/MemberService.java`, `S/loan/Retention.java` | – |
| Input validation, body size cap, SQL bind parameters | controllers (Bean Validation), `S/web/RequestSizeFilter.java`, `S/catalog/Catalog.java` | – |
| Uniform `problem+json` errors, no internals | `S/web/GlobalExceptionHandler.java`, `S/web/Problems.java` | code-only error mapping, no token logging: `A/api/ApiException.kt`, `A/api/Dtos.kt` |
| Security headers, CORS/CSRF, actuator | `S/security/SecurityConfig.java`, `server/src/main/resources/application.yml` | – |
| TLS only (1.2/1.3) | `application.yml`, dev cert from `server/scripts/dev-tls.sh` | `M/res/xml/network_security_config.xml`, `A/api/Net.kt` |
| Certificate pinning (release) | – | `A/api/Net.kt`, `android/app/build.gradle.kts` (`shelftap.apiPins`) |
| Secrets from the environment, startup refusal | `S/config/ShelfTapProperties.java`, `S/config/Secrets.java`, `server/.env.example` | – |
| Screenshot block, tapjacking filter | – | `A/MainActivity.kt` |
| No backup, only launcher exported, NFC foreground only | – | `M/AndroidManifest.xml`, `M/res/xml/data_extraction_rules.xml`, `A/nfc/TagReader.kt` |
| R8 obfuscation, log stripping | – | `android/app/build.gradle.kts`, `android/app/proguard-rules.pro` |

## Known limits

- Due-date reminders are set again only when the app is opened and the loans load: after the phone restarts, or after a book is returned at the desk, a reminder can be missing or can still fire once for a book already returned. A returned loan's alarm is not cancelled until the next load.
- An access token stays valid for up to 15 minutes after a block, a suspension or a password change. Refresh tokens are revoked at once.
- The rate limiter is in memory on one node and keys on the socket address.
- The member's CCCD number, date of birth and phone are stored in clear in PostgreSQL. Column encryption is a candidate next step.
- A release build without `shelftap.apiPins` is not pinned (Gradle prints a warning). There is no production server yet.
- The biometric lock has not been run on a phone with an enrolled fingerprint.
- Lockout keys on the login name, so someone can lock a victim out for 15 minutes; the per-IP limit slows this down.
