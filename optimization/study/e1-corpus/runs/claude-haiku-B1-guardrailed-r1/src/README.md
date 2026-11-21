# ShelfTap

Android catalogue app for library members (Kotlin, Jetpack Compose, Material 3, minSdk 26).
Package `vn.shelftap.app`, single app module `app`, plus a `baselineprofile` module.

## Behaviour

- **First launch** downloads the whole catalogue (`GET /api/titles`, 50 titles per page) into a
  local Room database. The download resumes from the last saved page after a failure, and the
  Catalogue screen shows progress and a Retry button.
- **Browsing and search work offline** once the download has completed. The UI only reads the
  local database.
- **Catalogue**: all titles ordered by title, paged with Paging 3.
- **Search**: a debounced (300 ms) full-text query over title, authors, ISBN and subjects.
  Each new query cancels the previous one. Empty results show "No books found".
- **Title detail**: opens from a row, shows the large cover, all fields and subjects as chips.
  It refreshes the title with `GET /api/titles/{id}` in the background, and keeps the local copy
  if the network is unavailable.

## Build

1. Open the folder in Android Studio (or run `gradle wrapper --gradle-version 8.9` once in the
   project root to create `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`
   and `gradle/wrapper/gradle-wrapper.jar`). The wrapper files are not in this project: the
   write of `gradle-wrapper.properties` was refused by a permission prompt.
2. Run `./gradlew :app:testDebugUnitTest` for the unit tests, `./gradlew :app:assembleRelease`
   for a minified build.
3. Generate the baseline profile with the `baselineprofile` module on a connected device:
   `./gradlew :app:generateBaselineProfile`.

The project was written without being compiled. Version numbers are pinned in
`gradle/libs.versions.toml`. Check them first if the build reports a missing artifact.

## Design notes

- Search uses an FTS4 table (`title_fts`, `unicode61` tokenizer). Matching is by word prefix:
  "tol" finds "Tolkien", but "kien" does not, because FTS indexes whole words, not substrings.
- The FTS rowid equals the rowid of the matching `title` row. Re-syncing deletes old index rows
  by rowid. Running `VACUUM` manually can change implicit rowids and would break that link,
  so the app never runs it.
- Cover URLs that are not `https://` are dropped. Cleartext traffic is disabled in
  `network_security_config.xml`.
- Catalogue data is public, so it is stored in plain app-private storage. Anything that
  becomes personal or per-user (tokens, loans, profile) must be encrypted with an
  Android Keystore AES-GCM key, per the project rules.
- Known limits: the catalogue is not refreshed after the first complete download. A title
  whose JSON does not parse stops the download and marks it failed.
