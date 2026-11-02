# Smart card development — source of truth

Everything about the chips ShelfTap talks to lives here: docs, specs, standards notes, APDU traces, test cards.

Scope:
- Book tags: ISO 15693 (`NfcV`), ISO 28560 data model, AFI/EAS anti-theft bit. No APDUs.
- Member cards: DESFire / JavaCard, or the phone via HCE. APDU challenge-response.
- Member onboarding: reading the chip ID card (CCCD, ICAO 9303).

Security decisions about keys and tag passwords belong in `../security/`; this folder records how the chips work.
