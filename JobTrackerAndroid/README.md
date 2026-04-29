# Job Tracker Android

Native Android client for the Job Tracker Spring Boot backend. Built for memory- and CPU-constrained devices (≥ Android 8.0).

## Stack

- **Kotlin 2.0** + **Jetpack Compose** (Material 3)
- **Retrofit + OkHttp + kotlinx.serialization**
- **Room 2.6** (KSP) for read cache
- **Manual DI** (`AppContainer`) — no Hilt/Koin
- **Hand-drawn Compose Canvas charts** (no Vico/MPAndroidChart)
- **EncryptedSharedPreferences** for JWT storage

minSdk **26** · targetSdk **35** · Java 17 toolchain.

## Prerequisites

1. **Android Studio** (Hedgehog or newer) — download from https://developer.android.com/studio
2. The first launch installs the Android SDK + Platform-Tools automatically. Use the SDK Manager to ensure **Android SDK Platform 35** is installed.
3. Create an AVD via **Tools → Device Manager**:
   - Pixel 6 / API 34 / arm64-v8a (Apple Silicon) or x86_64 (Intel)
   - For low-end testing: a custom device with **1.5–2 GB RAM, 2 cores**

## Open the project

In Android Studio: **File → Open** and pick `JobTracker/JobTrackerAndroid/`. Wait for Gradle sync to finish (~2 min on first run).

## Run the backend

The app expects the existing Spring Cloud Gateway at port 8080.

```bash
cd ../backend
docker compose up
```

## Run the app

- **Emulator**: hit Run (▶). Default base URL is `http://10.0.2.2:8080/v1/` — the emulator's alias for the host machine's `localhost`.
- **Physical device**: connect via USB with developer options enabled. Open **Profile → Debug: override base URL**, enter your Mac's LAN IP (e.g. `http://192.168.1.42:8080/v1/`), Save.

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

Covers `IdempotencyInterceptor`, `HostSelectionInterceptor`, and `AuthApi` JSON parsing.

## Release build

```bash
./gradlew :app:assembleRelease
```

R8 full mode + resource shrinking are enabled. Target APK size is **< 5 MB**.

## Architecture quick map

```
core/
  network/         OkHttp + Retrofit, Auth/Idempotency/Host/Unauthorized interceptors
  auth/            AuthRepository, SessionManager
  data/db/         Room: AppDatabase, ApplicationDao, ApplicationEntity, Converters
  data/prefs/      TokenStore (EncryptedSharedPreferences)
  domain/model/    Application, AppStatus, StatsSummary, TrendResponse
  ui/theme/        Material 3 theme

feature/
  login/           LoginScreen + ViewModel
  register/        RegisterScreen + ViewModel
  applications/
    list/          ApplicationsScreen, FilterBar, AppRow, StatusChip + ViewModel
    create/        NewApplicationScreen + ViewModel
    detail/        ApplicationDetailScreen + ViewModel
  dashboard/       DashboardScreen + ViewModel + charts/{TrendLineChart, StatusBreakdownChart}
  profile/         ProfileScreen + DebugOverrideScreen + ViewModels
  nav/             AppNavHost, Routes
```

## Notes

- The Spring backend's `/v1/auth/register` and `/v1/auth/token` issue HS256 JWTs valid for 24h. There is no refresh endpoint — on 401, the user is signed out and routed to Login.
- All POST/PATCH/DELETE on `/v1/applications` automatically receive a unique `Idempotency-Key` UUID via `IdempotencyInterceptor` — ViewModels never deal with it.
- Status breakdown and 12-week trend charts are rendered with Compose Canvas, not a chart library.
- Out-of-scope for this MVP: Alerts, CSV import, AI insights, ghosting tracker, top companies, role counts, activity feed.
