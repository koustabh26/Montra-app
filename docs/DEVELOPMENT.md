# Development guide

## Setup

1. Install JDK 17 and Android Studio (or the Android command-line tools with platform 35).
2. Clone the repository and open it in Android Studio, or use the wrapper from a shell.
3. Ensure `local.properties` contains `sdk.dir=...` (git-ignored).

## Common commands

| Task | Command |
|---|---|
| Unit tests | `./gradlew testDebugUnitTest` |
| Debug APK | `./gradlew assembleDebug` |
| Install on connected device | `./gradlew installDebug` |
| Lint | `./gradlew lintDebug` |
| Release APK (needs signing vars) | `./gradlew assembleRelease` |

Test reports: `app/build/reports/tests/testDebugUnitTest/index.html`.

## Project conventions

- Kotlin style per `.editorconfig` (4-space indent, LF line endings).
- Business rules go in `domain/` as pure functions with unit tests.
- Forms use `FormSheet` so the keyboard never hides inputs.
- Amounts are `Long` paise; use `util/Money.kt` for parsing and display.
- User-visible strings that appear in several places belong in `res/values/strings.xml`.

## Adding a database field or table

1. Edit `data/Entities.kt` (and DAO if needed).
2. Bump `version` in `AppDatabase.kt` and add a `Migration`. The current schema (v1) has no migrations, and `exportSchema` is `false`; consider enabling schema export when the first migration is written.
3. Update `BackupManager` (JSON/CSV) and its version handling.
4. Add tests.

## Debugging

Use Android Studio's Logcat. Do not log PINs or financial values.

## Known gaps

- No instrumented/UI tests.
- No database migration tests.
- Single `AppViewModel` is large; splitting per feature is a reasonable refactor.

## Gradle wrapper permissions

If you upload files from Windows, `gradlew` can lose its executable bit and CI fails with "Permission denied". Fix once from any clone:

```bash
git update-index --chmod=+x gradlew
git commit -m "Make gradlew executable"
```

The CI workflows also run `chmod +x gradlew` as a safeguard.
