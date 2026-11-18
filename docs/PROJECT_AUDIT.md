# Project audit

Audit of the repository contents as prepared for open-source publication. Based on reading the source, not on running the app.

| # | Item | Finding |
|---|---|---|
| 1 | Name / purpose | Montra, an offline Android expense tracker in ₹. |
| 2 | Problem solved | Private personal expense tracking without accounts or cloud. |
| 3 | Target users | Individuals tracking rupee spending; developers wanting a small Compose + Room codebase. |
| 4 | Features | Dashboard, Transactions, Categories (28 expense / 10 income defaults), Budgets, Analytics, Savings Goals, Settings & Backup; Day/Month/Year views. |
| 5 | Frontend | Jetpack Compose, Material 3, custom dark "glass" theme. |
| 6 | Backend | None. All logic is on-device. |
| 7 | Database | Room/SQLite `montra.db`, schema v1, no migrations, `exportSchema=false`. |
| 8 | Authentication | Local 6-digit PIN (PBKDF2 + Keystore HMAC), lockout, relock on background. |
| 9 | APIs / integrations | None. No `INTERNET` permission, no network code. |
| 10 | Languages / frameworks | Kotlin 2.0.21; Compose BOM 2024.12.01; Room 2.6.1; KSP. |
| 11 | Dependencies / package manager | Gradle 8.9 (wrapper added), AGP 8.7.3; AndroidX core, Activity, Lifecycle, Compose and Room libraries + JUnit 4.13.2 for tests. |
| 12 | Build / deploy | Gradle; CI workflows `ci.yml` and manual `release.yml` (added). No auto-deploy. |
| 13 | Tests | 23 JVM unit tests (`FinanceCalcTest` 10, `PeriodAndSecurityTest` 13). No instrumented/UI tests. **Not executed in the preparation environment** (no Android SDK / Maven access). |
| 14 | Existing docs | None before this work (no README, no LICENSE). |
| 15 | Environment configuration | Only the four optional signing env vars; no `.env`. |
| 16 | Git | The folder was **not a Git repository**; there is no history or branch structure to preserve. A remote named `koustabh26/Chuthiya` was mentioned by the user but was not reachable from the preparation environment. |

## Gaps found and how they were handled

| Gap | Action |
|---|---|
| No README | Added. |
| No LICENSE | **Not added** – needs maintainer decision; see `LICENSE_RECOMMENDATION.md`. |
| No Gradle wrapper | Added (Gradle 8.9). Its checksum was not independently verified against the official distribution; consider `./gradlew wrapper --gradle-version 8.9` locally to regenerate. |
| Weak `.gitignore` | Extended (keystores, `.env`, IDE, logs). |
| No CI for PRs | Added `ci.yml`. Old `build.yml` replaced. |
| No community files / templates | Added. |
| Name inconsistency (`Chuthiya` repo vs Montra app) | Rename suggested in `PRESENTATION.md`. |

## Risks and technical debt

- Source from the rename onward (v3–v5) has not been compiled by the preparer; the maintainer should build and run the tests first.
- No Room migrations; any schema change needs one.
- Unencrypted database and backups.
- Single large ViewModel; no UI tests.
- Legacy string `CHUTHIYA` is intentionally retained in `BackupManager` to restore old backups.
