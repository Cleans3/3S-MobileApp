# LibraryApp

Android app for library members. The loans feature lives in its own Android
library module (`:loans`) that the `:app` module depends on.

## Modules

- `:app` – application shell (`com.example.library`), hosts `LoansApp`.
- `:loans` – Android library (`com.example.library.loans`) containing:
  - `domain/` – `Loan`, `Book`, renewal rules (`Loan.renew`), `LoanRepository`.
  - `data/` – `InMemoryLoanRepository` and sample data.
  - `ui/` – Compose screens and ViewModels: active loans (with renew),
    reading history, and book details.
  - `LoansApp.kt` – navigation graph entry point.

## Features

- See the books you currently have on loan, sorted by due date.
- Renew a loan (max 2 renewals, each extends the due date by 14 days;
  overdue or returned loans cannot be renewed).
- Browse history of returned loans.
- Open any book's details.

## Building

The Gradle wrapper JAR is not included. Open the project in Android Studio
(it will sync the wrapper), or run `gradle wrapper --gradle-version 8.9` once
with a local Gradle installation, then `./gradlew :app:assembleDebug`.

Unit tests for the renewal rules: `./gradlew :loans:testDebugUnitTest`.
