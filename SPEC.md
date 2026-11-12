# ShelfTap: product spec (draft v1, 2026-11-12)

ShelfTap runs one library both at the desk and online. A Java Card smart card is the member's library card. Course deliverables per subject live in `smart-card/requirements.md`, `optimization/` and `security/`.

## 1. Actors

| Actor | Where | Does |
|---|---|---|
| Guest | Android app | Searches the catalog |
| Member | Desk, kiosk, Android app | Borrows, returns, renews, reserves, sees their loans and fines |
| Librarian | Desktop app | Issues cards, checks books in and out, manages the catalog, members and fines, unblocks cards |
| Admin | Desktop app | Manages librarian accounts, loan policy and card keys |

## 2. System parts

```
 Java Card ──APDU── Desktop app (librarian desk + self-service kiosk) ──HTTPS──┐
 (library card)     JavaFX, javax.smartcardio, USB card reader                 │
     └──NFC─────── Android member app (card login, C7)                          │
                                                                               ├── Server ── PostgreSQL
 Book tag ──NFC──── Android member app ───────────────────────────────HTTPS──┘   REST API, source of truth
 (ISO 15693)
```

| Part | Language | Why |
|---|---|---|
| `card/` applet | Java (Java Card Classic subset) | Only language Java Card supports |
| `desktop/` | Java + JavaFX | `javax.smartcardio` ships with the JDK; same language as the applet |
| `server/` | Java + PostgreSQL | Shares the domain model and crypto checks with the desktop app |
| `android/` | Java or Kotlin, decided when it starts | Only Jetpack Compose would force Kotlin |

The server is the only thing that decides. The desktop and Android apps request actions; the server checks policy and records them. Loans are never stored on the card or decided on the phone.

### 2.1 Hardware

| Item | Why |
|---|---|
| Dual-interface Java Card (JCOP, e.g. J3R180 or J3H145), unfused, default GlobalPlatform keys | Contact for the desk, contactless for the phone. Plain "NFC cards" (MIFARE Classic, NTAG) cannot run an applet. |
| USB smart card reader on the PC | Installs the applet (GlobalPlatformPro needs a PC reader) and is how the graded desktop app talks to the card |
| The member's Android phone with NFC | Reads the card for card login, reads book tags for self-checkout |
| jCardSim | Development and tests without a card |

The phone is not the desk's card reader. Relaying the desktop's APDUs through a phone would send the PIN over the network, add a second app to every desk session, and still leave the applet install needing a PC reader.

Books carry a barcode for the desk (a USB scanner, or typed in) and an NFC tag for phone self-checkout. A desk reader for ISO 15693 tags is not needed.

## 3. The library card

### 3.1 What the card holds

| Field | Stored | Readable |
|---|---|---|
| Card ID (serial) | Clear | Always |
| Member ID, full name, date of birth, CCCD number, phone, member type, expiry date | AES-encrypted | After a correct PIN |
| Member photo (JPEG, resized by the desktop app to at most 8 KB) | AES-encrypted | After a correct PIN |
| RSA private key | Generated on the card, never leaves it | Never |
| RSA public key | Clear | Always (also registered on the server at issue) |
| PIN | `OwnerPIN`, try limit 3 | Never |
| Data key (AES) | Wrapped twice: under a PIN-derived key and under the card's admin key | Never |

The server keeps the master copy of the member profile. The card copy lets the desk identify a member without a network round trip; when they differ, the server wins and the desk rewrites the card.

### 3.2 Card functions

| # | Function | Who | Checkpoint |
|---|---|---|---|
| C1 | Issue: write the card ID and profile, set the first PIN, generate the RSA key pair, return the public key | Librarian | 1 (profile + PIN), 2 (keys, encryption) |
| C2 | Verify PIN; 3 wrong tries block the card | Member | 1 |
| C3 | Change PIN (old PIN required); rewraps the data key only | Member | 1 |
| C4 | Unblock and reset PIN with the admin key, after the librarian checks the member's ID | Librarian | 1 |
| C5 | Read profile (decrypted on the card, after PIN) | Desk, kiosk | 1 (clear), 2 (encrypted) |
| C6 | Update profile (after PIN, re-encrypted on the card) | Librarian | 1 |
| C7 | Authenticate the card: sign a random challenge from the host with the RSA private key; the host checks it with the public key from the server | Desk, kiosk | 2 |
| C8 | Report state: PIN tries left, blocked or not | Desk, kiosk | 1 |
| C9 | Write photo, in chunks of one short APDU each (after PIN or at issue) | Librarian | 1 (clear), 2 (encrypted) |
| C10 | Read photo, in chunks (after PIN) | Desk, kiosk | 1 |

Revoking a lost card happens on the server (card status `lost`), not on the card. A revoked card fails at C7 because the server refuses its card ID.

### 3.3 Session at the desk or kiosk

1. Member inserts the card. The app reads the card ID (clear) and asks the server for its status and public key.
2. The app sends a random challenge; the card signs it (C7). Bad signature or revoked card: stop.
3. The member enters the PIN (C2). The app shows tries left on failure, and the "card blocked, see a librarian" screen when tries run out.
4. The app reads the profile (C5) and photo (C10), shows the photo so staff can match the face, and opens the member's session with the server.

Exact APDUs, key sizes and algorithms go in `smart-card/card-spec.md`, fixed after checking what jCardSim and the target card support.

## 4. Desktop app (physical library)

One JavaFX app with two modes.

**Librarian mode** (librarian logs in with an account):
- Members: register with a photo from a file (identity checked against the CCCD card), edit, suspend, search.
- Cards: issue (C1, C9), re-issue, mark lost, unblock (C4), update the profile and photo on the card (C6, C9).
- Circulation: check out (card session + scan book barcodes), check in, renew, view a member's loans and fines.
- Catalog: add and edit titles and copies, assign each copy its barcode and NFC tag ID.
- Fines: see overdue fines, record payment.
- Reports: overdue loans, popular titles, active members.

**Kiosk mode** (no librarian account, runs at a self-service desk):
- Member card session (§3.3), then borrow, return, renew, change PIN (C3), see loans and fines.

## 5. Android member app (online)

- Log in with an account, or by tapping the library card to the phone (card authentication C7 through the phone).
- Catalog search that works offline (local index), title details with cover.
- Reserve a title, cancel a reservation, get notified when it is ready.
- See current loans, due dates, renew.
- Self-checkout: tap the book's tag, the app requests the loan, the server records it.
- Reading history, with an option to turn it off.
- Due-date and reservation notifications.

## 6. Server

- REST API for the desktop and Android apps, PostgreSQL storage.
- Owns policy: loan period, max loans, renewals, reservations, fines.
- Holds card public keys and card status; checks every card authentication.
- Gate check endpoint: given a book's tag ID, says whether that copy is on loan (the gate itself is simulated).
- Audit log of every circulation and card action.

### 6.1 Data model

- `member` (profile, type, status, account login)
- `card` (card ID, member, public key, admin key reference, status: active / blocked / lost / revoked, issued at)
- `title` (ISBN, title, authors, subjects, cover) and `copy` (title, barcode, tag ID, shelf, status)
- `loan` (copy, member, out at, due at, returned at, renewals)
- `reservation` (title, member, placed at, ready at, status)
- `fine` (loan, amount, paid at)
- `staff` (librarian and admin accounts)
- `audit_log` (who, what, when, from which device)

### 6.2 Loan policy (defaults, set in config)

14-day loans, at most 5 at once, 1 renewal if nobody has reserved the title, overdue fine per day per book, no new loans while a fine is unpaid.

## 7. Privacy and security constraints

- Reading history is personal data under Decree 13/2023: the member can turn it off, and returned loans lose their member link after a retention period.
- The card's personal data is useless without the PIN; its admin key and the server's database never travel to a member device.
- The 6-digit PIN-derived key could be brute-forced from a dumped chip; that protection rests on the chip's tamper resistance and the 3-try limit. The threat model lives in `security/`.

## 8. Out of scope

Real gate hardware, online payment, e-books, a web front end, multiple library branches.

A stored-value balance on the card is out too. Fines are already owed to the server, so a card balance would be a second money ledger that has to resist cloning and rollback. The course does not ask for it. Fines are paid at the desk and recorded on the server.

## 9. Decisions

- 2026-11-12: real dual-interface Java Card + USB reader on the PC; the phone reads the card only for its own login (§2.1).
- 2026-11-12: books use a barcode at the desk and an NFC tag on the phone (§2.1).
- 2026-11-12: no stored-value balance on the card (§8).
- 2026-11-12: member photo stored on the card, encrypted, not printed (§3.1).
