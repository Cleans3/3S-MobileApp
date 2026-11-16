#!/usr/bin/env bash
# Makes the dev TLS keystore for https://localhost:8443 and the Android emulator (10.0.2.2).
# Usage: SHELFTAP_TLS_KEYSTORE_PASSWORD=... scripts/dev-tls.sh [extra-LAN-IP]
# Output goes to server/dev-tls/ (gitignored): shelftap-dev.p12 for the server, shelftap-dev.crt for clients to trust.
set -euo pipefail

: "${SHELFTAP_TLS_KEYSTORE_PASSWORD:?set SHELFTAP_TLS_KEYSTORE_PASSWORD first (see .env.example)}"
cd "$(dirname "$0")/.."

san="dns:localhost,ip:127.0.0.1,ip:10.0.2.2"
if [ -n "${1:-}" ]; then
  san="$san,ip:$1"
fi

mkdir -p dev-tls
rm -f dev-tls/shelftap-dev.p12 dev-tls/shelftap-dev.crt

keytool -genkeypair -alias shelftap-dev -keyalg RSA -keysize 2048 -sigalg SHA256withRSA -validity 365 \
  -storetype PKCS12 -keystore dev-tls/shelftap-dev.p12 -storepass:env SHELFTAP_TLS_KEYSTORE_PASSWORD \
  -dname "CN=localhost, O=ShelfTap dev" -ext "san=$san" -ext "eku=serverAuth"

keytool -exportcert -rfc -alias shelftap-dev -keystore dev-tls/shelftap-dev.p12 \
  -storepass:env SHELFTAP_TLS_KEYSTORE_PASSWORD -file dev-tls/shelftap-dev.crt

echo "keystore: dev-tls/shelftap-dev.p12   certificate to trust: dev-tls/shelftap-dev.crt   SAN: $san"
