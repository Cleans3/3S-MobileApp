# Library Member Sign-In & Account Android App

A complete native Android application implementing member sign-in, persistent authentication ("stay signed in"), and account profile viewing for a public library system communicating with a REST API.

---

## Features

- **Member Sign In**:
  - Secure authentication with email and password via library REST API (`POST /api/v1/auth/login`).
  - Form validation with interactive error indicators for invalid emails and blank/short passwords.
  - Quick demo sign-in chip (`member@library.org`) for immediate evaluation.
- **Persistent Session ("Stays Signed In")**:
  - Remembers login credentials and JWT token across app restarts using local `SessionManager`.
  - Configurable "Stay signed in on this device" preference.
  - Automatic `Authorization: Bearer <token>` injection for all secure REST requests via OkHttp `AuthInterceptor`.
  - Secure sign-out with confirmation dialog that revokes the session and clears local credentials.
- **Member Profile & Digital Library Card**:
  - Digital library card with member number, barcode simulation, expiration date, and membership status (`ACTIVE`, `EXPIRED`).
  - Active checkout stats (Books checked out, Holds ready for pickup, Fines balance).
  - List of currently borrowed books with due dates, days remaining, and overdue warnings.
  - Membership details (Email, phone, home branch, join date).
  - Manual pull/button refresh from the REST API (`GET /api/v1/members/me`).
- **REST API Integration**:
  - Standard Retrofit 2 + OkHttp 4 client architecture.
  - Built-in `MockLibraryApiInterceptor` allowing the app to run and demonstrate full functionality immediately without requiring an external server, while still performing genuine HTTP request/response pipeline handling.

---

## REST API Specification

### 1. Sign In
- **Endpoint**: `POST /api/v1/auth/login`
- **Headers**: `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "email": "member@library.org",
    "password": "libraryPass123"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "token": "jwt_lib_token_...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "memberId": "MEM-78291",
    "email": "member@library.org",
    "fullName": "Eleanor Vance",
    "memberNumber": "LIB-2024-88492"
  }
  ```

### 2. Get Profile
- **Endpoint**: `GET /api/v1/members/me`
- **Headers**: `Authorization: Bearer <token>`
- **Response (200 OK)**:
  ```json
  {
    "id": "MEM-78291",
    "memberNumber": "LIB-2024-88492",
    "fullName": "Eleanor Vance",
    "email": "member@library.org",
    "phoneNumber": "+1 (555) 349-8812",
    "membershipType": "General Resident (Adult)",
    "membershipStatus": "ACTIVE",
    "expiryDate": "2027-12-31",
    "joinedDate": "2021-04-15",
    "homeBranch": "Central Metropolitan Library",
    "activeLoansCount": 3,
    "holdsReadyCount": 1,
    "outstandingFees": 0.00,
    "borrowedBooks": [
      {
        "id": "book-01",
        "title": "The Midnight Library",
        "author": "Matt Haig",
        "isbn": "978-0525559474",
        "borrowDate": "2026-11-05",
        "dueDate": "2026-11-26",
        "daysRemaining": 5,
        "isOverdue": false
      }
    ]
  }
  ```

### 3. Sign Out
- **Endpoint**: `POST /api/v1/auth/logout`
- **Headers**: `Authorization: Bearer <token>`
- **Response (200 OK)**:
  ```json
  {
    "status": "logged_out"
  }
  ```

---

## Project Structure

```
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/library/account/
│       │   │   ├── LibraryApplication.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/
│       │   │   │   ├── api/
│       │   │   │   │   ├── ApiClient.kt
│       │   │   │   │   ├── AuthInterceptor.kt
│       │   │   │   │   ├── LibraryApiService.kt
│       │   │   │   │   └── MockLibraryApiInterceptor.kt
│       │   │   │   ├── model/
│       │   │   │   │   ├── AuthModels.kt
│       │   │   │   │   └── ProfileModels.kt
│       │   │   │   ├── repository/
│       │   │   │   │   └── AuthRepository.kt
│       │   │   │   └── session/
│       │   │   │       └── SessionManager.kt
│       │   │   └── ui/
│       │   │       ├── auth/
│       │   │       │   ├── AuthViewModel.kt
│       │   │       │   └── LoginScreen.kt
│       │   │       ├── navigation/
│       │   │       │   └── AppNavigation.kt
│       │   │       ├── profile/
│       │   │       │   ├── ProfileScreen.kt
│       │   │       │   └── ProfileViewModel.kt
│       │   │       └── theme/
│       │   │           ├── Color.kt
│       │   │           ├── Theme.kt
│       │   │           └── Type.kt
│       │   └── res/
│       │       ├── values/
│       │       │   ├── colors.xml
│       │       │   ├── strings.xml
│       │       │   └── themes.xml
│       │       └── xml/
│       │           ├── backup_rules.xml
│       │           └── data_extraction_rules.xml
│       └── test/java/com/library/account/
│           └── AuthRepositoryTest.kt
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
└── gradlew.bat
```

---

## Tech Stack
- **Language**: Kotlin 1.9.23
- **UI Framework**: Jetpack Compose (Material 3)
- **Networking**: Retrofit 2.11.0, OkHttp 4.12.0, Gson 2.10.1
- **Architecture**: MVVM with Repository Pattern, Coroutines, StateFlow
- **Session Persistence**: Android SharedPreferences with cached profile support
