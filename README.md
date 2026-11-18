<!-- LOGO PLACEHOLDER: add docs/assets/logo.png and replace this comment with an <img> tag. No logo file exists yet. -->

# Montra

**A private, offline-first expense tracker for Android, with amounts in Indian rupees (₹).**

Montra keeps your income, expenses, budgets and savings goals on your phone. It has no account, no cloud sync, no ads, and no network permission.

> **Status:** pre-release. Version `1.0.0` is declared in `app/build.gradle.kts`, but no release has been published. See [CHANGELOG.md](CHANGELOG.md).

## Overview

Montra is a native Android app written in Kotlin with Jetpack Compose. All data is stored in a local Room (SQLite) database. A mandatory 6-digit PIN protects the app.

## Problem statement

Many expense apps require an account and upload your spending to a server. Montra takes the opposite approach: everything stays on the device, and you move data in and out yourself using backup and CSV files.

## Objectives

- Record spending and income quickly.
- Show spending by day, month or year in plain language.
- Keep data private by default (no network access).
- Remain easy to audit, build and extend.

## Target users

People who want a simple, private rupee-based tracker, and developers who want a small Compose + Room codebase to read or extend.

## Key features

| Module | What it does |
|---|---|
| **Home (Dashboard)** | Balance and spending summary for the selected Day, Month or Year, plus a plain-language "pace" card. |
| **History (Transactions)** | Add, edit and delete transactions; grouped by date; Undo on delete. |
| **Categories** | 28 expense and 10 income default categories; add your own. |
| **Budgets** | An overall monthly budget and per-category budgets. |
| **Insights (Analytics)** | Trends and category breakdowns for Day, Month or Year. |
| **Goals (Savings)** | Savings goals with progress. |
| **Settings & Backup** | Change PIN, screen privacy, JSON backup and restore, CSV export. |

Other behaviour:

- Money is stored as whole **paise** (`Long`) to avoid floating-point error.
- Six-digit PIN with weak-PIN rejection and escalating lockout after repeated failures.
- App relocks when it goes to the background.
- Optional screen privacy (blocks screenshots and recents thumbnails).
- CSV export neutralises spreadsheet formula injection.

## Screenshots

_No screenshots have been added yet._ Capture them from a real build and place them in `docs/assets/screenshots/`, then reference them here.

## Demo

_No demo is available._

## Technology stack

| Area | Choice |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.12.01), Material 3 |
| Persistence | Room 2.6.1 (KSP 2.0.21-1.0.28) |
| Architecture | MVVM: one `AppViewModel`, a repository, `StateFlow` |
| Build | Gradle 8.9 (wrapper), Android Gradle Plugin 8.7.3 |
| Android | minSdk 26, compileSdk / targetSdk 35 |
| JDK | 17 |

## Architecture overview

UI (Compose screens) → `AppViewModel` → `Repository` → Room DAOs → SQLite. Pure calculation logic lives in `domain/` and is unit-tested. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Installation requirements

- JDK 17
- Android SDK (platform 35, build-tools) – via Android Studio or command-line tools
- Android Studio (recent stable) is recommended
- An Android 8.0+ (API 26) device or emulator

## Step-by-step installation

```bash
git clone <YOUR-REPOSITORY-URL>
cd Montra
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`. Install it with `adb install -r <apk>` or copy it to a device. Full details: [docs/INSTALLATION.md](docs/INSTALLATION.md).

## Environment variable configuration

Montra needs **no environment variables to run or debug**. Release signing uses four optional variables (`MONTRA_KEYSTORE`, `MONTRA_STORE_PASSWORD`, `MONTRA_KEY_ALIAS`, `MONTRA_KEY_PASSWORD`). See [docs/CONFIGURATION.md](docs/CONFIGURATION.md). There is no `.env.example` because there is no runtime configuration file.

## Local development

Open the project in Android Studio and run the `app` configuration, or use `./gradlew installDebug`. See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).

## Production build

```bash
export MONTRA_STORE_PASSWORD=...   # never commit these
export MONTRA_KEY_PASSWORD=...
./gradlew assembleRelease
```

See [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md). Releases are built and signed by you; nothing deploys automatically.

## Folder structure

```
Montra/
├── app/
│   └── src/
│       ├── main/java/com/montra/app/
│       │   ├── data/        Room entities, DAOs, repository, backup
│       │   ├── domain/      FinanceCalc, Period (pure logic)
│       │   ├── security/    PinPolicy, LockoutPolicy, SecurePrefs
│       │   ├── ui/          AppRoot, components, theme, screens
│       │   ├── util/        Money formatting
│       │   ├── AppViewModel.kt
│       │   └── MainActivity.kt
│       └── test/java/com/montra/app/   unit tests
├── docs/                    project documentation
├── gradle/wrapper/          Gradle wrapper
├── .github/                 workflows, issue and PR templates
└── README.md, LICENSE (pending), CONTRIBUTING.md, ...
```

## API documentation

Not applicable: Montra has no server, no API and no network code.

## Database setup

Automatic. Room creates `montra.db` on first launch. Schema: [docs/DATABASE.md](docs/DATABASE.md).

## Authentication

A local 6-digit PIN, stored as a salted PBKDF2-HMAC-SHA256 hash sealed with an Android Keystore key. It protects access to the app on the device; it does **not** encrypt the database or backup files. See [SECURITY.md](SECURITY.md).

## Testing

```bash
./gradlew testDebugUnitTest
```

The repository contains 23 JVM unit tests (`FinanceCalcTest`, `PeriodAndSecurityTest`). There are no instrumented or UI tests. Test results are not claimed here; run them yourself or check the CI run.

## Troubleshooting

| Problem | Try |
|---|---|
| `SDK location not found` | Create `local.properties` with `sdk.dir=/path/to/Android/Sdk` (it is git-ignored). |
| Wrong JDK | Use JDK 17 (`java -version`). |
| `./gradlew: Permission denied` | `chmod +x gradlew` |
| Release build fails on signing | Set the signing variables, or build debug. |
| Forgot PIN | There is no recovery by design; clear app data (this deletes all data) or restore from a backup. |

More in [docs/FAQ.md](docs/FAQ.md).

## Roadmap

See [docs/ROADMAP.md](docs/ROADMAP.md). Items there are ideas, not promises.

## Contributing

Contributions are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) and the [Code of Conduct](CODE_OF_CONDUCT.md).

## Security

Report vulnerabilities privately; see [SECURITY.md](SECURITY.md).

## License

**No license has been chosen yet.** Until the maintainer adds a `LICENSE` file, all rights are reserved by default and others have no permission to reuse the code. Options are discussed in [docs/LICENSE_RECOMMENDATION.md](docs/LICENSE_RECOMMENDATION.md).

## Acknowledgements

Built with [Kotlin](https://kotlinlang.org/), [Jetpack Compose](https://developer.android.com/jetpack/compose) and [Room](https://developer.android.com/training/data-storage/room).

## Support

See [SUPPORT.md](SUPPORT.md).

## Maintainer

GitHub: `koustabh26` _(taken from the connected account; maintainer to confirm before publishing)_.
