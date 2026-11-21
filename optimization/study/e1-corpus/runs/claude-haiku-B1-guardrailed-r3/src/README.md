# ShelfTap catalogue (Android)

Kotlin, Jetpack Compose with Material 3, minSdk 26, targetSdk/compileSdk 35, single Gradle module `app`, package `vn.shelftap.app`.

## Status

The sources were written by hand and have **not been compiled or run**: no build, no network access, and no emulator were used. The first build may surface small API mismatches; fix them against the pinned versions in `gradle/libs.versions.toml`.

## Building

1. The Gradle wrapper jar and `gradle/wrapper/gradle-wrapper.properties` are not in this tree. Generate them once with a local Gradle 8.9 install: `gradle wrapper --gradle-version 8.9`. Or open the folder in Android Studio, which does this on import.
2. Open the project in Android Studio (Ladybug or newer) or run `./gradlew :app:assembleDebug`.

## Structure

```
app/src/main/java/vn/shelftap/app/
  ShelfTapApplication.kt   app start; starts the resumable catalogue download
  AppContainer.kt          manual dependency container (Room, OkHttp, API, repository, sync)
  MainActivity.kt
  model/Title.kt           domain model
  data/
    local/                 Room entities, DAOs, database (titles, FTS4 title_search, sync_state)
    remote/CatalogueApi.kt HTTPS calls to /api/titles and /api/titles/{id}; org.json parsing
    CatalogueSync.kt       one-time full download, one transaction per page, resumable
    TitleRepository.kt     paged local queries, FTS query builder, detail lookup
    TitleMapping.kt        entity <-> domain mapping
  ui/
    ShelfTapRoot.kt        Scaffold, bottom tabs, NavHost (catalogue, search, title/{id})
    CatalogueScreen.kt, SearchScreen.kt, DetailScreen.kt
    Components.kt          paged list, row, cover, availability badge, sync banner
    ViewModels.kt          catalogue, search (debounced), detail; ViewModel factory
    theme/Theme.kt
app/src/main/res/          strings, theme, adaptive launcher icon, network security config
app/src/main/baseline-prof.txt  hand-written baseline profile (see deviations)
```

## Behaviour and decisions

- **Offline:** the first launch downloads the whole catalogue (50 titles per request, about 2,000 requests for 100,000 titles). Each page and the saved "next page" marker commit together, so an interrupted download resumes from the last committed page. If it stops, a banner offers Retry. After completion, browsing and searching use only the local Room database.
- **Catalogue screen:** all titles ordered by title, paged by 50 through Paging 3. Each row shows the cover, title, authors, year, and an "Available" or "All copies out" badge.
- **Search:** the text is debounced 250 ms, and each new query cancels the previous one (`flatMapLatest`). Matching uses the FTS4 index over title, authors, ISBN and subjects. Each typed word matches as a word prefix, and all words must match. Matching is word-based, not arbitrary substring. Accents are compared as typed, because the FTS simple tokenizer does not fold diacritics. Empty input shows the full list. No matches show "No books found".
- **Detail screen:** the local copy is shown first. `GET /api/titles/{id}` is called only if the title is not stored locally. Shows the large cover, all fields, subjects as chips, and a back button.
- **Covers:** loaded with Coil `AsyncImage`, which caches them on disk, so covers seen once remain available offline. Only `https://` cover URLs are accepted.
- **List request:** the sync calls `GET /api/titles?page=&size=50` without `q`, because the whole catalogue is needed. The spec did not say whether `q` can be omitted. If the server requires it, pass an empty `q`.

## Rule compliance (CLAUDE.md)

- HTTPS only. The network security config sets `cleartextTrafficPermitted="false"`. No trust-all code. Default TLS is used.
- No secrets, tokens or personal data are stored or logged. The catalogue is public, so Keystore encryption does not apply. Nothing is logged.
- Explicit `android:exported` on the only component (the launcher activity).
- Repositories are `google()` and `mavenCentral()` only. Dependencies are in `gradle/libs.versions.toml`.
- No deprecated APIs were knowingly used. `AsyncTask` and `SharedPreferences` are not used. `security-crypto` is not used.
- Lazy lists use stable keys and content types. Long lists are lazy. Parsing and mapping run on `Dispatchers.IO`, which is injected. No `runBlocking`.
- Icon-only controls have string-resource descriptions. Icons next to text use `null`. Clickable rows are at least 48 dp high.
- R8 is on for release (`isMinifyEnabled` and `isShrinkResources`, with `proguard-android-optimize.txt`).

## Deviations and open items

- **Baseline Profile:** the rule asks for a separate `baselineprofile` module. The request asked for one Gradle module. I kept one module and shipped a hand-written `app/src/main/baseline-prof.txt` (class-level rules for start-up and list classes) with `androidx.profileinstaller`. This is weaker than a generated profile. To comply fully, add a `baselineprofile` test module that generates the profile, and apply the `androidx.baselineprofile` plugin.
- **Gradle wrapper:** see Building above.
- **Search matching** is word-prefix matching, not substring matching. Substring matching on 100,000 titles would need a full scan or a trigram index.
- **Catalogue refresh:** the full download happens once. Later changes on the server are not pulled. A refresh action would need a new sync marker.
