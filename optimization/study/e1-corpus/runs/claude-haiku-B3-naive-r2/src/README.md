# Library Loans (Android)

Two modules:

- `:feature-loans` – Android **library** module containing the loans feature
  (borrowed books, renewal, reading history, book details). Entry point: `LoansFeature()`.
- `:app` – application module that hosts the feature in `MainActivity`.

Stack: Kotlin 2.0.21, Jetpack Compose (BOM 2024.09.00), Material 3, Navigation Compose,
ViewModel + StateFlow. Data is served by `InMemoryLoanRepository` with sample data.

Renewal rules (`RenewalPolicy`): returned and overdue loans cannot be renewed; at most
2 renewals per loan; each renewal adds 14 days to the due date.

Setup: the Gradle wrapper JAR is not included. Run `gradle wrapper --gradle-version 8.9`
once (or open the project in Android Studio) before building.
