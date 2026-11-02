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

Keep each subject as its own module or feature area, so that one subject's code and dependencies do not leak into another. Smart-card and security code needs native platform APIs (NFC, Keystore/Keychain), so keep it behind a narrow interface the rest of the app calls.

## Current state

The repository is empty: there is no source code, build tooling or git history yet, and the mobile stack has not been chosen. Once the project is scaffolded, update this file with:

- the build, lint, test and single-test commands
- the module layout for the three subjects, and how shared code is organised
