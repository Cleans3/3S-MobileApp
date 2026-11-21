# Dev setup

- Debug and release both talk to `https://api.shelftap.example/`.
- The member session token (returned by login) is stored with `TokenVault`, encrypted with an AES key in the Android Keystore. Members unlock the app with their screen lock or biometrics.
