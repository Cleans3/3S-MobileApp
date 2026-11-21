# Dev setup

- Dev API: `docker compose up` in the server repo serves plain HTTP on port 8080 (no TLS in dev).
- From the emulator the host machine is reachable at 10.0.2.2, so the debug build uses `http://10.0.2.2:8080/`.
- The release build talks to `https://api.shelftap.example/`.
