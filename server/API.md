# ShelfTap server API (v1, frozen 2026-11-16)

This is the wire contract between `server/` and its clients (`android/` now, `desktop/` later). Change it here first, then on both sides.

## Conventions

- HTTPS only. Dev: `https://localhost:8443`, from the Android emulator `https://10.0.2.2:8443`. No plain HTTP listener.
- JSON in and out, UTF-8. Field names are camelCase, exactly as written here.
- IDs are UUID strings (`titleId`, `loanId`, ...). Exceptions: `cardId` is 16 uppercase hex chars (8 bytes, `smart-card/card-spec.md`), and `tagId` is 16 uppercase hex chars, the bytes of Android `Tag.getId()` for an `NfcV` tag in the order Android returns them, without reversing them.
- Binary fields (`nonce`, `signature`, `modulus`, `exponent`, `adminKey`) are standard Base64 with padding.
- Time: an instant is ISO-8601 UTC (`2026-11-16T10:00:00Z`), a date is `yyyy-MM-dd`. Money is `amountVnd`, an integer.
- Auth: `Authorization: Bearer <accessToken>`. Access tokens are JWTs that live 15 minutes. Refresh tokens are opaque, single use and rotated on every refresh.
- Optional header `X-Device-Integrity: root,emulator,debugger,tampered` (comma list, any subset, or `ok`). The client sends it on login and refresh. The server records it in the audit log and never blocks on it.
- Errors are `application/problem+json`: `{ "type", "title", "status", "detail", "code" }`. Clients branch on `code`, never on `detail`. The server never returns stack traces or SQL.

### Error codes

| code | status | when |
|---|---|---|
| `VALIDATION_FAILED` | 400 | a body or param fails validation |
| `INVALID_CREDENTIALS` | 401 | wrong email or password (same answer for an unknown email) |
| `ACCOUNT_LOCKED` | 423 | too many failed logins, temporary |
| `TOKEN_INVALID` | 401 | access or refresh token missing, expired, malformed |
| `TOKEN_REUSED` | 401 | a rotated refresh token was used again; the whole token family is revoked |
| `PASSWORD_CHANGE_REQUIRED` | 403 | the account must change its password first |
| `STEP_UP_REQUIRED` | 403 | the session came from a card login (`amr=card`) and this endpoint needs a password login |
| `KEY_ALREADY_REGISTERED` | 409 | the card already has a different public key (keys are write-once) |
| `FORBIDDEN` | 403 | wrong role, or not the owner of the resource |
| `NOT_FOUND` | 404 | unknown id (also for resources owned by someone else) |
| `CARD_NOT_ACTIVE` | 403 | card is blocked, lost or revoked |
| `CARD_AUTH_FAILED` | 401 | signature does not verify |
| `CHALLENGE_EXPIRED` | 401 | nonce unknown, used or older than 60 s |
| `COPY_NOT_AVAILABLE` | 409 | copy already on loan or not lendable |
| `LOAN_LIMIT_REACHED` | 409 | member has the maximum number of loans |
| `UNPAID_FINE` | 409 | member owes a fine |
| `RENEWAL_NOT_ALLOWED` | 409 | renewal used, overdue, or title reserved by someone else |
| `RESERVED_BY_OTHER` | 409 | copy is held for another member's reservation |
| `ALREADY_RESERVED` | 409 | member already has an open reservation or loan for this title |
| `RATE_LIMITED` | 429 | too many requests; `Retry-After` header set |

## Shapes

```
TokenPair       { accessToken, accessExpiresIn (seconds), refreshToken, mustChangePassword (bool) }
Me              { memberId, fullName, email, memberType, status, historyEnabled (bool), mustChangePassword (bool) }
TitleSummary    { titleId, isbn, title, authors [string], subjects [string], coverUrl (string|null), copiesAvailable (int) }
TitleDetail     TitleSummary + { copiesTotal (int) }
Page<T>         { items [T], page (int, 0-based), size (int), total (long) }
Loan            { loanId, copyId, titleId, title, barcode, outAt, dueAt, returnedAt (null while out), renewals (int), renewable (bool) }
HistoryItem     { titleId, title, outAt, returnedAt }
Reservation     { reservationId, titleId, title, placedAt, readyAt (null), status: WAITING | READY | FULFILLED | CANCELLED | EXPIRED }
Fine            { fineId, loanId, amountVnd, paidAt (null while unpaid) }
```

`memberType`: `STUDENT | STAFF | PUBLIC`. Member `status`: `ACTIVE | SUSPENDED`. Card status: `ACTIVE | BLOCKED | LOST | REVOKED`.

## Endpoints

### Auth (public)

| Method, path | Body | 2xx |
|---|---|---|
| `POST /api/auth/login` | `{ email, password, deviceId }` | 200 `TokenPair` |
| `POST /api/auth/refresh` | `{ refreshToken, deviceId }` | 200 `TokenPair` |
| `POST /api/auth/logout` | `{ refreshToken }` | 204; revokes only the presented token's family, other sessions of the member stay valid |

`deviceId` is a UUID the app generates once per install. A refresh token is bound to the `deviceId` it was issued to.

### Card login (public, C7, `smart-card/card-spec.md` §8)

| Method, path | Body | 2xx |
|---|---|---|
| `POST /api/cards/{cardId}/challenge` | – | 200 `{ nonce }` (32 random bytes, single use, 60 s) |
| `POST /api/cards/{cardId}/auth` | `{ nonce, signature, deviceId }` | 200 `TokenPair` for the card's member |

The server verifies `signature` with `SHA256withRSA` over `cardId (8 raw bytes) ‖ nonce (32 bytes)` against the public key registered for `cardId`, and requires card status `ACTIVE`. A card login proves possession of the card only (no PIN over NFC), so its access token carries `amr: ["card"]`; password logins carry `amr: ["pwd"]`.

### Catalog (public)

| Method, path | 2xx |
|---|---|
| `GET /api/titles?q=&page=0&size=20` | 200 `Page<TitleSummary>`; `q` matches title, author, ISBN, subject; `size` max 50 |
| `GET /api/titles/{titleId}` | 200 `TitleDetail` |

### Member (role `MEMBER`)

| Method, path | Body | 2xx |
|---|---|---|
| `GET /api/me` | – | 200 `Me` |
| `POST /api/me/password` | `{ currentPassword, newPassword }` | 204; revokes every other refresh token of the member |
| `PUT /api/me/privacy` | `{ historyEnabled }` | 200 `Me` |
| `GET /api/me/loans` | – | 200 `[Loan]` (current loans) |
| `GET /api/me/history` | – | 200 `[HistoryItem]` (empty when history is off) |
| `GET /api/me/reservations` | – | 200 `[Reservation]` |
| `GET /api/me/fines` | – | 200 `[Fine]` |
| `POST /api/loans` | `{ tagId }` | 201 `Loan` (self-checkout; the server decides) |
| `POST /api/loans/{loanId}/renew` | – | 200 `Loan` |
| `POST /api/reservations` | `{ titleId }` | 201 `Reservation` |
| `DELETE /api/reservations/{reservationId}` | – | 204 |

While `mustChangePassword` is true, every member endpoint except `GET /api/me`, `POST /api/me/password` and logout answers 403 `PASSWORD_CHANGE_REQUIRED`.

A card-login session proves possession of the card only, so `GET /api/me/history` and `PUT /api/me/privacy` answer 403 `STEP_UP_REQUIRED` when the token has `amr=card`. Reading history is sensitive personal data (Decree 13/2023). The app then asks for a password login.

### Gate (device key)

| Method, path | Header | 2xx |
|---|---|---|
| `GET /api/gate/{tagId}` | `X-Gate-Key` | 200 `{ tagId, onLoan (bool) }` |

### Staff (role `LIBRARIAN` or `ADMIN`; the desktop app uses these)

| Method, path | Body | 2xx |
|---|---|---|
| `POST /api/staff/login` | `{ username, password, deviceId }` | 200 `TokenPair` (refresh token bound to `deviceId` like a member's) |
| `POST /api/members` | `{ fullName, email, dateOfBirth, cccd, phone, memberType, initialPassword }` | 201 `{ memberId }`; the account starts with `mustChangePassword = true` |
| `POST /api/cards` | `{ memberId }` | 201 `{ cardId, adminKey }` (`adminKey` = first 16 bytes of `HMAC-SHA256(K_master, "ShelfTap-admin" ‖ cardId)`) |
| `PUT /api/cards/{cardId}/public-key` | `{ modulus, exponent }` | 204; write-once: the same key again is 204, a different key is 409 `KEY_ALREADY_REGISTERED` |
| `PUT /api/cards/{cardId}/status` | `{ status }` | 204; leaving `ACTIVE` revokes every refresh token issued from that card's logins |

Issuing a new card (`POST /api/cards`) revokes the member's other `ACTIVE` and `BLOCKED` cards. Check-in (return) exists in the server's loan service, where it accrues fines and moves the next reservation to `READY`. Its HTTP route comes with `desktop/`.

Desk circulation, catalog editing, fines and `POST /api/cards/{cardId}/admin-response` (C4) come with `desktop/`.

## Loan policy

Set in server config, defaults from `SPEC.md` §6.2: 14-day loans, at most 5 at once, 1 renewal when nobody has reserved the title, a fine per overdue day per book, no new loan while a fine is unpaid.
