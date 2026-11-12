# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

3S-MobileApp is a mobile app that serves three subjects:

1. **Smart card development**: working with smart cards from the device (NFC / contactless cards, APDU exchange).
2. **Mobile app optimization**: performance work such as startup time, rendering, memory, battery and app size.
3. **Mobile app security**: secure storage, transport security, platform hardening and protecting the app against tampering.

The app that brings the three subjects together is **ShelfTap**, an NFC smart library app. Members search the catalog and self-checkout by tapping a book's tag. Librarians check books in and out, register members and take shelf inventory with a phone.

- **Chips:** book tags are ISO 15693 (`NfcV`, ISO 28560 data model, AFI/EAS anti-theft bit) and do not use APDUs. Member cards (DESFire/JavaCard, or the phone via HCE) use APDU challenge-response. New members are identity-checked by reading their chip ID card (CCCD, ICAO 9303).
- **Key security rule:** the member app never holds a tag password and never decides whether a book may leave. It only requests a loan; the server records it, and the gate or staff kiosk checks loan status with the server. Reading history is sensitive personal data (Decree 13/2023).
- **Optimization topics (course list):** 1 UI/graphics and 2 memory/resources are the main topics, supported by 6 computation (offline full-text search), 7 performance + security, 3 scheduling/multithreading and 5 AI. Every optimization is measured before and after on a low-cost device against these targets: offline search over 100k books under 50 ms, catalog scrolling at 60 fps with covers, memory under 150 MB while browsing, self-checkout tap under 1 s.

Each subject has a source-of-truth folder at the repo root for its docs and specs: `smart-card/`, `optimization/`, `security/`. Write subject material in its own folder, never in another subject's.

The `lab/` folder at the repo root is for storing labs. Put each lab there, not in a subject folder, as `lab/labN/<subject>/` (`smart-card`, `optimization` or `security`, since each subject numbers its labs separately), with a `README.md` that starts with the lab's date and subject.

Keep each subject as its own module or feature area, so that one subject's code and dependencies do not leak into another. Smart-card and security code needs native platform APIs (NFC, Keystore/Keychain), so keep it behind a narrow interface the rest of the app calls.

## Current state

The product spec is `SPEC.md`. The system is a Java Card applet, a JavaFX desktop app, a Java server with PostgreSQL, and an Android app. So far only the card exists.

Build: Gradle 9.4.1 through the wrapper, one multi-project build rooted at `settings.gradle`, running on JDK 21.

| Module | What | Status |
|---|---|---|
| `card/` | Java Card 2.2.2 applet `shelftap.card.LibraryCardApplet`, the wire contract in `smart-card/card-spec.md` | Built, 17 jCardSim tests |
| `desktop/`, `server/`, `android/` | See `SPEC.md` §2 | Not started |

Commands:
- All tests: `./gradlew test`
- Card tests only: `./gradlew :card:test`
- One test: `./gradlew :card:test --tests 'shelftap.card.LibraryCardAppletTest.threeWrongPinsBlockTheCard'`

Card code rules:
- Applet code is compiled with `--release 8` against the Java Card API that jCardSim ships. Use only the Java Card subset: `byte`, `short` and `boolean`, no `int`/`long`/`String`, no allocation outside the constructor.
- `smart-card/card-spec.md` §9 lists the jCardSim quirks the applet works around. Read it before touching RSA or APDU lengths.
- A CAP file for the real card is not built yet. That needs the Oracle Java Card SDK and ant-javacard, and gets added when the card arrives.
