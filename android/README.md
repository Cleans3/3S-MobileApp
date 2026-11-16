# ShelfTap Android member app

Kotlin + Jetpack Compose. Barebone on purpose: plain UI, real security. It is the base the security course integrates on. Wire contract: `../server/API.md`. Card contract: `../smart-card/card-spec.md` §8 ("Android card login").

Screens: password login, card login (NFC), forced password change, biometric unlock, catalog search and title detail (reserve), my loans (renew), self-checkout (tap a book tag), reservations, reading history, account (privacy toggle, biometric lock, change password, logout).

## Build

`android/` is its own Gradle build (own wrapper, Gradle 9.6, AGP 9.5, Kotlin 2.4, JDK 21). The root build does not include it and does not need the Android SDK.

```
cd android
./gradlew assembleDebug assembleRelease testDebugUnitTest
```

- The SDK is found through `ANDROID_HOME` or `android/local.properties` (`sdk.dir=...`, git-ignored).
- compileSdk is 37 (current Compose, lifecycle, navigation, coil and OkHttp require it); targetSdk is 36, minSdk 26. With the SDK licences accepted, AGP downloads the platform by itself on the first build.
- Release has R8 (minify + shrink) on and is unsigned. Sign it yourself; no keystore lives in the repo (`*.jks` and `*.keystore` are git-ignored).
- On-device tests (Keystore vault), needs an emulator or phone: `./gradlew connectedDebugAndroidTest`.

## Run against the dev server

1. Start `server/` (see `../server/README.md`). It listens on `https://localhost:8443` only, no plain HTTP.
2. Make the dev TLS certificate with `../server/scripts/dev-tls.sh`. The certificate must list `localhost` and `10.0.2.2` as subject alternative names, or hostname verification fails from the emulator.
3. Install that certificate on the emulator or phone as a user CA (Settings, Security, Encryption and credentials, Install a certificate, CA certificate). Debug builds trust user CAs; release builds trust system CAs only.
4. `./gradlew installDebug`. The debug build talks to `https://10.0.2.2:8443/` by default (the host machine as seen from the emulator). A real phone needs `-Pshelftap.apiBaseUrl=https://<host>:8443/` with a certificate for that host.

The debug build needs no certificate file and no property to compile.

## Gradle properties

Set with `-P` or in `~/.gradle/gradle.properties`. They end up in `BuildConfig`.

| Property | Default | Meaning |
|---|---|---|
| `shelftap.apiBaseUrl` | debug `https://10.0.2.2:8443/`, release a placeholder host | API base URL. Must be `https://` and end with `/`. A real release build must set it. |
| `shelftap.apiPins` | empty | Comma list of `sha256/<base64>` SPKI pins for the API host. **Release only**; debug never pins. Empty means no pinning, and the build prints a warning. |
| `shelftap.signingCertSha256` | empty | SHA-256 (hex) of the release signing certificate. Empty skips the tamper check. |
| `shelftap.inactivityMinutes` | 5 | Idle minutes before auto-logout. |

A pin, from the server certificate: `openssl s_client -connect HOST:443 -servername HOST </dev/null 2>/dev/null | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | base64`, prefixed with `sha256/`. Pin a backup key too, so a certificate rotation does not lock every installed app out.
The signing certificate digest: `apksigner verify --print-certs app-release.apk`.

## Security mechanisms

| Mechanism | Where |
|---|---|
| Refresh token sealed with AES-256-GCM, key non-exportable in the Android Keystore, StrongBox when the device has one (falls back to the TEE) | `security/KeystoreRefreshTokenVault.kt`, blob format in `security/AesGcm.kt` |
| Token store is DataStore; the access token is never stored, memory only | `KeystoreRefreshTokenVault.kt`, `session/SessionManager.kt` (`access`) |
| Optional biometric lock: a strong-biometric-bound Keystore key (needed for every use, invalidated when biometrics change) unwraps the secret that seals the token, so rotation works without a prompt but the token cannot be read from disk without the user | `KeystoreRefreshTokenVault.kt` (`beginEnableLock` ...), `security/BiometricLock.kt`, `ui/LockScreen` in `ui/AuthScreens.kt`, toggle in `ui/AccountScreen.kt` |
| Locks when the app goes to the background; a changed fingerprint resets the lock and forces a new login | `MainActivity.onStop`, `SessionManager.lockIfEnabled` / `unlock` |
| Auto-logout after inactivity, including time spent in the background | `session/InactivityTracker.kt`, `MainActivity.logoutIfIdle` |
| Cleartext off everywhere (manifest, network security config, OkHttp connection spec); TLS 1.2/1.3 only, no redirects | `AndroidManifest.xml`, `res/xml/network_security_config.xml`, `api/Net.kt` |
| Release trusts system CAs only; debug additionally trusts user CAs (dev certificate) | `res/xml/network_security_config.xml` (`debug-overrides`) |
| Certificate pinning in release | `api/Net.kt` (`CertificatePinner`), `app/build.gradle.kts` (`shelftap.apiPins`) |
| `FLAG_SECURE` on the one window all screens live in: no screenshots, recordings or Recents thumbnail; touches ignored while overlaid | `MainActivity.onCreate` |
| No backups, no device-to-device transfer | `allowBackup=false`, `res/xml/data_extraction_rules.xml` |
| Only the launcher activity is exported; the exported `ProfileInstallReceiver` that Compose brings in is removed | `AndroidManifest.xml` |
| NFC only in the foreground (reader mode while a tag screen is resumed); no tech or NDEF intent filters | `nfc/TagReader.kt`, `NfcReaderEffect` in `ui/Common.kt` |
| R8 minify and resource shrinking; `Log` calls stripped in release (including libraries) | `app/build.gradle.kts`, `app/proguard-rules.pro` |
| Device integrity signals (root, emulator, debugger or debuggable, tampered signing certificate) sent as `X-Device-Integrity`, shown as a warning. The app never blocks on it | `security/Integrity.kt`, `security/IntegrityChecker.kt`, `SessionManager.login/refresh/cardLogin`, `IntegrityBanner` |
| One refresh and one retry on `401 TOKEN_INVALID`; `TOKEN_REUSED` wipes everything and goes to login | `api/Net.kt` (`TokenAuthenticator`), `SessionManager.refreshAfterUnauthorized` / `doRefresh` |
| Tokens and passwords never logged: request DTOs mask `toString`, errors carry status and code only | `api/Dtos.kt`, `api/ApiException.kt` |
| Card session is step-up gated: `403 STEP_UP_REQUIRED` on history or privacy opens a password dialog (own `FLAG_SECURE`), swaps in a password session and revokes the card session's refresh token; cancelling or a wrong password keeps the card session | `SessionManager.stepUpLogin`, `StepUpDialog` in `ui/AuthScreens.kt`, `HistoryViewModel` in `ui/MemberScreens.kt`, `AccountViewModel.setHistoryEnabled` |
| Passwords are never saved to instance state | `ui/AuthScreens.kt` |
| Per-install random `deviceId`, bound to the refresh token server-side | `data/DeviceIdStore.kt` |
| The app never holds a tag password and never decides whether a book may leave: self-checkout sends the tag ID and shows the server's answer; no tag write, no AFI/EAS | `nfc/TagId.kt`, `ui/MemberScreens.kt` (`CheckoutViewModel.tagTapped`), `data/Repository.checkout` |
| Card login sends no PIN and reads no profile: SELECT, GET STATUS, AUTHENTICATE x2 only | `nfc/Apdu.kt`, `SessionManager.cardLogin` |
| Covers load through the same pinned, TLS-only client, without the bearer token | `ShelfTapApp.newImageLoader` |

## Tests

- JVM (`testDebugUnitTest`): the API client against MockWebServer (refresh and rotation, one retry, `TOKEN_REUSED`, error-code mapping, integrity header), session and biometric-lock states against a fake vault, the card flow against a scripted card, APDU bytes, step-up login, tag ID formatting, AES-GCM format, integrity decisions, inactivity clock.
- Device (`connectedDebugAndroidTest`): the real Keystore vault, covering round trip, no plaintext on disk, non-exportable key, tampered blob, clear. The lock-key test is skipped unless a strong biometric is enrolled.

By hand, with a biometric enrolled: turn on Biometric lock in Account, send the app to the background and reopen it (the unlock prompt must appear), then enrol another fingerprint (the app must ask for a fresh login).

## Limits

- No offline catalog index (`SPEC.md` §5); search calls the server. That is the optimization course's job.
- No push notifications for due dates or reservations yet.
- The refresh token is replaced on the server before it is stored here; killing the process in that window leaves a stale token, and the next start ends in `TOKEN_REUSED` and a fresh login.
