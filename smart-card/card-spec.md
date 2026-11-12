# ShelfTap library card: applet spec (v1, 2026-11-12)

This is the wire contract between the Java Card applet (`card/`) and its hosts: the desktop app over a USB reader, and the Android app over NFC. `SPEC.md` §3 says what the card is for; this file says exactly how it works. Function numbers C1–C10 are the ones in `SPEC.md` §3.2.

## 1. Platform

| Item | Choice | Why |
|---|---|---|
| API level | Java Card 2.2.2 (Classic) | jCardSim 2.2.2 is the only jCardSim on Maven Central and is what Lab 1 uses. JCOP 3 (J3H145) and JCOP 4 (J3R180) cards run 2.2.2 applets unchanged. |
| Simulator | jCardSim 2.2.2 | Development and automated tests. Its quirks are listed in §9. |
| Package AID | `F0 53 48 45 4C 46` | `F0` is an unregistered proprietary RID (ISO/IEC 7816-5), then ASCII `SHELF` |
| Applet AID | `F0 53 48 45 4C 46 54 41 50 01` | Package AID + `TAP` + version `01` |
| APDUs | Short APDUs only, CLA `80` | Every response is 255 bytes or less (§9, quirk 1) |

## 2. Cryptography

| Use | Algorithm | Java Card API |
|---|---|---|
| Data encryption (profile, photo) | AES-128-CBC, no padding on the card, random IV per write | `Cipher.ALG_AES_BLOCK_128_CBC_NOPAD` |
| Data key (DK) | 16 random bytes made at issue, never leaves the card | `RandomData.ALG_SECURE_RANDOM` |
| DK at rest | Stored only in two wrapped copies: `wrapPin = AES-128-ECB(KEK_pin, DK)` and `wrapAdmin = AES-128-ECB(K_admin, DK)` | `Cipher.ALG_AES_BLOCK_128_ECB_NOPAD` (one random block, so ECB leaks nothing) |
| DK in use | Unwrapped into a transient key after a correct PIN or admin login, and cleared on select and deselect | `KeyBuilder.TYPE_AES_TRANSIENT_DESELECT` |
| PIN key | `KEK_pin = first 16 bytes of SHA-256(salt ‖ PIN)`, where `salt` is 16 random bytes stored at issue | `MessageDigest.ALG_SHA_256` |
| Admin key | `K_admin`, 16 bytes per card, set at issue. The server derives it as `first 16 bytes of HMAC-SHA256(K_master, "ShelfTap-admin" ‖ cardId)`. Only the server holds `K_master`. | `AESKey` (persistent) |
| Card key pair | RSA-2048 CRT, generated on the card at issue, public exponent 65537 | `KeyPair.ALG_RSA_CRT`, `LENGTH_RSA_2048` |
| Card signature (C7) | RSASSA-PKCS1-v1_5 with SHA-256 over `cardId ‖ nonce`. The card builds the EMSA-PKCS1-v1_5 block itself and applies the raw RSA private-key operation. | `MessageDigest.ALG_SHA_256` + `Cipher.ALG_RSA_NOPAD`. `ALG_RSA_SHA_256_PKCS1` does not exist before Java Card 3.0.1, and `Cipher.ALG_RSA_PKCS1` signs wrongly on jCardSim (§9). |

The host verifies a C7 signature with standard `Signature.getInstance("SHA256withRSA")`. The server rebuilds the public key from the modulus and the exponent.

Wrapping DK under the PIN only adds protection if an applet bug ever skips the PIN check. Against someone who dumps the chip, `OwnerPIN` and the 10⁶-value PIN space give no protection, so this rests on the chip's tamper resistance (`SPEC.md` §7).

## 3. PIN

- Exactly 6 ASCII digits (`30`–`39`). Anything else is rejected with `6A80`.
- `OwnerPIN(3, 6)`. Its try counter is persistent and tear-safe: it is decremented before the compare.
- The member picks the first PIN on the desk keypad at issue; the librarian never sets it. The desktop app refuses `000000`, `123456` and other single-digit or straight runs before sending.
- After 3 wrong tries the PIN is blocked (`6983`). Only an admin reset (C4) unblocks it. Card authentication (C7) still works while blocked, so the desk can recognise the card and send the member to a librarian.
- CHANGE PIN and RESET PIN rewrap DK and update the PIN inside one `JCSystem` transaction, so a card pulled mid-command cannot be left with a PIN and a `wrapPin` that disagree.

## 4. State and session

Persistent state: `BLANK` (`00`, applet installed and nothing written) or `ACTIVE` (`01`). "Blocked" is not a separate state; it is `ACTIVE` with 0 PIN tries left. `GET STATUS` reports it as `02`.

Session flags are transient and cleared in `select()` and on deselect:
- `pinOk`: the PIN was verified (`OwnerPIN.isValidated()`).
- `adminOk`: the admin challenge-response succeeded.
- `DK`: loaded whenever either flag is set.

**Contactless rule.** Over NFC (`APDU.getProtocol()` media is contactless type A or B) the applet accepts only SELECT, GET STATUS, GET PUBLIC KEY and AUTHENTICATE. Every other command returns `6986`, so a phone can never issue, rewrite or read a card, or try PINs on it.

## 5. Commands

`P` = needs `pinOk`; `A` = needs `adminOk`; `P|A` = either one. Lengths are in bytes.

| C# | Command | CLA INS P1 P2 | Data in | Data out | Needs | NFC |
|---|---|---|---|---|---|---|
| – | SELECT | `00 A4 04 00` | applet AID | – | – | yes |
| C8 | GET STATUS | `80 50 00 00` | – | 13: `cardId`(8) ‖ `state`(1) ‖ `triesLeft`(1) ‖ `flags`(1) ‖ `photoLen`(2) | – | yes |
| C1 | ISSUE | `80 10 00 00` | 30: `cardId`(8) ‖ `PIN`(6) ‖ `K_admin`(16) | – | state `BLANK` | no |
| C2 | VERIFY PIN | `80 20 00 00` | 6: PIN | – | state `ACTIVE` | no |
| C3 | CHANGE PIN | `80 22 00 00` | 12: old PIN ‖ new PIN | – | – (old PIN checked, counts as a try) | no |
| C4 | GET CHALLENGE | `80 24 00 00` | – | 16: random challenge (single use) | state `ACTIVE` | no |
| C4 | ADMIN AUTH | `80 26 00 00` | 16: `AES-128-ECB(K_admin, challenge)` | – | a challenge is pending | no |
| C4 | RESET PIN | `80 28 00 00` | 6: new PIN | – | A | no |
| C5 | READ PROFILE | `80 30 00 00` | – | padded profile, ≤ 192 | P | no |
| C6 | UPDATE PROFILE | `80 32 00 00` | padded profile, 16–192, multiple of 16 | – | P\|A | no |
| C9 | PHOTO BEGIN | `80 40 00 00` | 2: padded length `L` (16–8192, multiple of 16) | – | P\|A | no |
| C9 | PHOTO WRITE | `80 42 offH offL` | chunk, 16–240, multiple of 16 | – | P\|A | no |
| C10 | PHOTO READ | `80 44 offH offL` | – (Le = `F0`) | `min(240, L − offset)` bytes | P | no |
| C7 | AUTHENTICATE | `80 60 00 00` | 32: nonce | signature bytes 0–127 | state `ACTIVE` | yes |
| C7 | AUTHENTICATE (part 2) | `80 60 01 00` | – | signature bytes 128–255 | right after part 1 | yes |
| C7 | GET PUBLIC KEY | `80 62 P1 00` | – | `P1=00`: modulus bytes 0–127; `01`: bytes 128–255; `02`: exponent (3) | key generated | yes |

`flags` bits: b0 `pinOk`, b1 `adminOk`, b2 key pair present, b3 photo complete.

### 5.1 What each command does

- **ISSUE (C1)** stores `cardId` and `K_admin`, sets the PIN, makes a random `salt` and DK, and writes both wrapped copies. It generates the RSA key pair (checkpoint 2), sets the state to `ACTIVE`, and validates the new PIN so the same session can write the profile and photo. The command is rejected with `6985` once the state is `ACTIVE`. Re-issuing a physical card means deleting and reinstalling the applet with GlobalPlatformPro; the server marks the old card ID `revoked`.
- **VERIFY PIN (C2)** returns `9000`, `63Cx` (wrong PIN, x tries left) or `6983` (blocked). On success it unwraps DK with `KEK_pin`.
- **CHANGE PIN (C3)** checks the old PIN (`63Cx` / `6983` as for VERIFY), then rewraps DK under the new `KEK_pin` and updates the PIN, all in one transaction.
- **Admin login (C4)**: the card gives a 16-byte challenge. The desktop sends it to the server (`POST /api/cards/{cardId}/admin-response`, librarian session required, audit-logged), and the server returns `AES-128-ECB(K_admin, challenge)`. ADMIN AUTH compares the response in constant time and consumes the challenge whether it matches or not. On success it sets `adminOk` and unwraps DK from `wrapAdmin`; on failure it returns `6982`. `K_admin` is a 128-bit key and every challenge is single use, so no attempt counter is needed.
- **RESET PIN (C4)** sets the new PIN, which also resets the try counter to 3, rewraps `wrapPin`, and validates the new PIN.
- **Profile (C5, C6)**: the host builds the TLV (§6) and pads it with ISO/IEC 7816-4 padding (`80`, then `00` up to a multiple of 16). The card stores `IV ‖ AES-CBC(DK, IV, data)` with a fresh IV on every update. On read it returns the padded plaintext, and the host strips the padding. With no profile stored, READ returns `6A88`.
- **Photo (C9, C10)**: the host resizes the JPEG to at most 8191 bytes and pads it the same way, so `L` is at most 8192. PHOTO BEGIN clears the old photo and picks a new IV. PHOTO WRITE takes chunks in order: `offset` must equal the bytes written so far (`6B00` otherwise). Each chunk is encrypted in CBC mode, continuing from the last stored ciphertext block (the IV for offset 0), so no cipher state is kept between APDUs. PHOTO READ accepts any 16-aligned offset, because CBC decrypts from the previous ciphertext block. It needs the photo to be complete (`6985` otherwise).
- **AUTHENTICATE (C7)** computes `SHA-256(cardId ‖ nonce)`, builds `00 01 FF…FF 00 ‖ DigestInfo(SHA-256) ‖ hash` (256 bytes), applies the RSA private key, and keeps the signature in a transient buffer until part 2 is read. It needs no PIN: card authentication comes before the PIN, so a member never types a PIN into a fake card.

## 6. Profile TLV

Tag (1 byte), length (1 byte), value. The card does not parse it.

| Tag | Field | Format | Max |
|---|---|---|---|
| `01` | Member ID | ASCII | 16 |
| `02` | Full name | UTF-8 | 64 |
| `03` | Date of birth | ASCII `YYYYMMDD` | 8 |
| `04` | CCCD number | ASCII digits | 12 |
| `05` | Phone | ASCII | 15 |
| `06` | Member type | 1 byte: `01` student, `02` staff, `03` public | 1 |
| `07` | Card expiry | ASCII `YYYYMMDD` | 8 |

The worst case is 138 bytes, or 144 after padding, which fits the 192-byte limit.

## 7. Status words

| SW | Meaning |
|---|---|
| `9000` | OK |
| `63Cx` | Wrong PIN, `x` tries left |
| `6700` | Wrong data length |
| `6982` | Needs `pinOk` / `adminOk`, or the admin response was wrong |
| `6983` | PIN blocked |
| `6985` | Wrong state: ISSUE on an active card, no challenge pending, photo incomplete, part 2 without part 1 |
| `6986` | Command not allowed over contactless |
| `6A80` | Bad data: PIN not 6 digits, length not a multiple of 16 |
| `6A84` | Too large: profile over 192, photo over 8192 |
| `6A88` | No profile stored |
| `6B00` | Bad offset in P1P2 |
| `6D00` / `6E00` | Unknown INS / CLA |

## 8. Host flows

**Issue at the librarian desk (contact reader):**
1. The server creates the `card` row, assigns a random 8-byte `cardId`, and derives `K_admin`.
2. The member types the first PIN. The desktop sends ISSUE.
3. The desktop sends UPDATE PROFILE, then PHOTO BEGIN and PHOTO WRITE ×n.
4. The desktop reads GET PUBLIC KEY ×3 and the server stores the public key against `cardId`.

`K_admin` passes through the desktop's memory once, here. After issue it only exists on the card and as a derivation on the server.

**Desk/kiosk session (`SPEC.md` §3.3):**
1. SELECT, then GET STATUS gives `cardId`.
2. The desktop asks the server for a nonce: `POST /api/cards/{cardId}/challenge` returns 32 random bytes, single use, valid for 60 s.
3. AUTHENTICATE ×2. `POST /api/cards/{cardId}/auth {nonce, signature}`. The server checks the signature and that the card status is `active`.
4. VERIFY PIN, READ PROFILE, PHOTO READ ×n.

**Android card login:** steps 1–3 over NFC. The phone sends no PIN and reads no profile.

Over the contact reader the PIN travels in clear between the PC and the card; no secure channel is used. Malware on the desk PC is in the threat model (`security/`), not handled on the card.

## 9. jCardSim 2.2.2 quirks (probed 2026-11-12)

Each item was reproduced with a probe applet run on `jcardsim-2.2.2.jar` from Lab 1 (SHA-1 `9dfb3223…`).

| # | Quirk | How the applet handles it |
|---|---|---|
| 1 | A 256-byte response (`Le = 00`) fails with SW `0003` (`APDUException.BAD_LENGTH`); 255 bytes works | Every RSA-sized output is split into two 128-byte parts (§5) |
| 2 | `RSAPublicKey.getModulus` writes 257 bytes (a leading `00` sign byte) | Write into a 258-byte buffer and keep the last 256 bytes |
| 3 | `Cipher.ALG_RSA_PKCS1` with the private key builds a 61-byte padded block (`01`, 8×`FF`, `00`, data) and zero-fills the rest, so the signature fails `SHA256withRSA` verification | Not used. The applet builds the PKCS#1 v1.5 block itself (§2). |
| 4 | `Cipher.ALG_RSA_NOPAD` rejects a 256-byte input (`CryptoException.ILLEGAL_USE`) but accepts the same block as 255 bytes (leading `00` dropped). A real card needs 256. | The applet passes 256 and retries with the last 255 bytes on `ILLEGAL_USE`. Both forms give the same value, and the simulator output verified with `SHA256withRSA`. |
| 5 | `Signature.ALG_RSA_SHA_256_PKCS1` is missing from the 2.2.2 API | See quirk 3; SHA-256 itself (`MessageDigest.ALG_SHA_256`) works |
| 6 | Re-selecting the applet does not clear a `TYPE_AES_TRANSIENT_DESELECT` key | `select()` clears the session itself (§4) |
| 7 | `APDU.getProtocol()` always reports contact (`00`) | The contactless rule is only testable on the real card |
| 8 | jCardSim 3.x is not on Maven Central (only 2.2.1 and 2.2.2) and has no GitHub release | Stay on 2.2.2 and the Java Card 2.2.2 API |
| 9 | `APDU.setOutgoing()` does not return the command's Le | PHOTO READ always returns a fixed 240-byte chunk (less at the end) instead of honouring Le |
| 10 | `Simulator.installApplet(aid, class)` passes empty install parameters, so the usual `register(bArray, bOffset + 1, bArray[bOffset])` fails | Tests install with GlobalPlatform-style parameters (AID length, AID, empty privileges, empty applet data) |

**Check on the real card when it arrives:** RSA-2048 key generation time at ISSUE; that the 256-byte NOPAD path signs and verifies without the retry; that the contactless rule returns `6986` from the phone; and free EEPROM after ISSUE plus an 8 KB photo (about 10 KB is needed).

## 10. Checkpoint staging

| Checkpoint | Card scope |
|---|---|
| 1 (2026-11-30) | ISSUE without key generation, VERIFY / CHANGE / RESET PIN with the 3-try limit, admin challenge-response, GET STATUS, profile and photo stored in clear |
| 2 (2026-12-14) | DK and both wraps; profile and photo AES-encrypted; RSA key pair at ISSUE, GET PUBLIC KEY, AUTHENTICATE |
| 3 (2026-12-28) | No new card commands. The desktop app and the server complete the flows in §8. |

The host-side command set is the same at every checkpoint. Checkpoint 2 changes how the card stores data, not the wire format.

As of 2026-11-12 the applet in `card/` already implements checkpoints 1 and 2 in full; `./gradlew :card:test` runs 17 jCardSim tests against this contract.
