# Library account app

This Android app provides persistent member sign-in and a profile screen backed by a REST API.

The default API base URL is `https://api.library.example.com/`. The app expects:

- `POST /auth/login` with `{ "email": "...", "password": "..." }`, returning `token` or `access_token`.
- `GET /me` with an `Authorization: Bearer <token>` header, returning member profile fields.

Set `BuildConfig.API_BASE_URL` in `app/build.gradle.kts` for the library's deployment API.
