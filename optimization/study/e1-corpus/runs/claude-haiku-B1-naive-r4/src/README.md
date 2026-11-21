# Library Catalogue (Android)

Kotlin + Jetpack Compose app that lets library members browse the book catalogue,
see cover images and search by title, author or ISBN.

## Design

- **Paging 3** loads the catalogue 30 books at a time, so the ~100,000 books are never
  held in memory or downloaded at once.
- **Server-side search**: the query is sent to the API (`q=`); the app does not filter locally.
- **Search debounce** (300 ms) and `flatMapLatest` so only the latest query is fetched.
- **Coil** loads and caches cover thumbnails; a placeholder icon is shown while loading or on error.
- Layers: `data/` (Retrofit API, paging source, repository), `ui/` (ViewModel, Compose screens).

## API assumption

The app expects this contract (set the base URL in `app/build.gradle.kts`, field `LIBRARY_BASE_URL`):

```
GET {base}/books?page=1&per_page=30&q=dune
200 -> {
  "items": [
    { "id": "1", "title": "Dune", "author": "Frank Herbert", "year": 1965,
      "isbn": "9780441013593", "cover_url": "https://.../dune.jpg" }
  ],
  "page": 1,
  "total_pages": 3334
}
```

Adjust `data/LibraryApi.kt` and the DTOs if the library system differs.
The default URL `https://library.example.org/api/v1/` is a placeholder.

## Building

The Gradle wrapper JAR and `gradlew` scripts are not included. Either:

- open the project in Android Studio (it creates the wrapper automatically), or
- run `gradle wrapper --gradle-version 8.9` once with a local Gradle install.

Then build with `./gradlew assembleDebug`. Requires JDK 17 and Android SDK 35.

Note: `gradle/wrapper/gradle-wrapper.properties` was not written in this project
(the file write was not permitted). The wrapper command above generates it.
