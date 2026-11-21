# Dev setup

- Debug and release both talk to `https://api.shelftap.example/` (HTTPS, publicly trusted certificate).
- Release builds are minified with R8 (`isMinifyEnabled = true`, default rules from `proguard-android-optimize.txt` plus `app/proguard-rules.pro`). The debug build is not minified and works.
