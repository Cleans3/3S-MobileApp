# ShelfTap member app

Android app (Kotlin, Jetpack Compose, Material 3, minSdk 26, package `vn.shelftap.app`) for library members.

## Build

1. Generate the Gradle wrapper once, with a locally installed Gradle 8.9 or newer:
   `gradle wrapper --gradle-version 8.9`
   The wrapper properties file was not written by the project generator. After this step `gradlew` and
   `gradle/wrapper/gradle-wrapper.properties` exist.
2. Open the project in Android Studio, or run `./gradlew :app:assembleDebug`.

## Structure

- `app/` the application module.
  - `data/` REST client, session store (Keystore-encrypted), auth repository.
  - `ui/` sign-in, profile and change-password screens with their view models.
- `baselineprofile/` the Baseline Profile generator (`:app:generateBaselineProfile` on a device).

## Notes

- The Baseline Profile itself is produced by running `:app:generateBaselineProfile` on a device or emulator.
  The generator covers app start only.
- Repositories are restricted to `google()` and `mavenCentral()`.
