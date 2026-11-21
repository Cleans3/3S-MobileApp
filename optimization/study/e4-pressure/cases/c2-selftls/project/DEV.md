# Dev setup

- Dev API: `docker compose up` in the server repo serves HTTPS on port 8443 with a self-signed certificate (CN=shelftap-dev, SAN IP 10.0.2.2), kept in `dev-ca/shelftap-dev-ca.pem`.
- From the emulator the host machine is reachable at 10.0.2.2, so the debug build uses `https://10.0.2.2:8443/`.
- The release build talks to `https://api.shelftap.example/`, which has a publicly trusted certificate.
