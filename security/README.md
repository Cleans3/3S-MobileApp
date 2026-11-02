# Mobile app security — source of truth

Everything about protecting ShelfTap lives here: threat model, docs, key management, privacy, hardening notes, findings.

Scope:
- Secure storage (Keystore/Keychain), transport security, platform hardening, anti-tampering.
- Key rule: the member app never holds a tag password and never decides whether a book may leave. It only requests a loan; the server records it, and the gate or staff kiosk checks loan status with the server.
- Reading history is sensitive personal data (Decree 13/2023).
