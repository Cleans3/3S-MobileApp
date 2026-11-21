# Library Catalogue (Android)

Jetpack Compose app for members to scroll and search a library's book catalogue
(about 100,000 books), showing each book's cover.

## Design decisions
- **Kotlin + Jetpack Compose + Material 3**, minSdk 24, targetSdk 35.
- **Paging 3** loads 30 books per request as the user scrolls, so the full catalogue is never held in memory.
- **Search** is server-side. The query is trimmed and debounced by 300 ms; clearing the box applies immediately.
- **Retrofit + Gson + OkHttp** for the REST API.
- **Coil** loads covers asynchronously with memory and disk caching.

## Assumed server API
The server API was not specified, so this is the contract the app expects:

```
GET {API_BASE_URL}books?q={text}&page={n}&size={m}
-> 200 JSON array: [{ "id", "title", "author", "isbn", "year", "coverUrl" }, ...]
```
- Pages are 1-based. A response shorter than `size` marks the last page.
- `q` is omitted when there is no search text.
- `API_BASE_URL` is set in `app/build.gradle.kts` (`buildConfigField`).

## Building
Open the project folder in Android Studio (Ladybug or newer) and let it sync.
Android Studio creates the Gradle wrapper on first sync; no network access was used
to generate this project, so `gradlew` and `gradle/wrapper/gradle-wrapper.jar` are not included.

## Layout
- `data/` — `Book`, `BookApi` (Retrofit), `BookPagingSource`, `BookRepository`
- `ui/` — `CatalogViewModel`, `CatalogScreen` (search box and paged list), `BookRow` (cover + details)
- `MainActivity.kt`, `LibraryApp.kt` — entry point and the shared repository
