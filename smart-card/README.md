# Smart card development — source of truth

Everything about the chips ShelfTap talks to lives here: docs, specs, standards notes, APDU traces, test cards.

Scope:
- Book tags: ISO 15693 (`NfcV`), ISO 28560 data model, AFI/EAS anti-theft bit. No APDUs.
- Member card: Java Card applet, APDU challenge-response. Wire contract: [`card-spec.md`](card-spec.md).
- Member onboarding: reading the chip ID card (CCCD, ICAO 9303).

Course deliverables and deadlines: [`requirements.md`](requirements.md).

Security decisions about keys and tag passwords belong in `../security/`; this folder records how the chips work.
