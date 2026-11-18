# Architecture

Montra is a single-module Android app (`:app`, package `com.montra.app`) using MVVM.

```
 Compose screens (ui/)
        │  observe StateFlow / Compose state, call ViewModel functions
        ▼
   AppViewModel ─────────────► SecurePrefs (PIN, lockout)   [security/]
        │
        ▼
    Repository ──► BackupManager (JSON / CSV)                [data/]
        │
        ▼
   Room DAOs ──► SQLite (montra.db)                          [data/]

 domain/FinanceCalc + Period : pure functions, no Android dependencies
```

## Layers

| Layer | Path | Notes |
|---|---|---|
| UI | `ui/`, `ui/screens/`, `ui/components/`, `ui/theme/` | Screens: Dashboard, Transactions (History), Budgets, Analytics (Insights), Goals, Categories, Settings, Lock. Shared pieces: `FormSheet` (bottom-sheet forms), `GlassCard`, `PeriodSwitcher`. Theme: `MontraTheme`. |
| ViewModel | `AppViewModel.kt` | One ViewModel for the whole app. Holds UI state, the selected period, and one-shot `UiMessage` events (e.g. Undo snackbar). |
| Repository | `data/Repository.kt` | Single access point to DAOs; includes `restoreTransaction` for Undo. |
| Persistence | `data/Entities.kt`, `Daos.kt`, `AppDatabase.kt` | Room 2.6.1. |
| Domain | `domain/FinanceCalc.kt`, `domain/Period.kt` | Totals, trends, projections, period filtering. Unit-tested. |
| Security | `security/PinPolicy.kt`, `security/SecurePrefs.kt` | PIN rules, lockout schedule, hashed PIN storage. |
| Utilities | `util/Money.kt` | Paise ⇄ rupee formatting and input sanitising. |

## Key design decisions

- **Money as `Long` paise.** Avoids floating-point rounding. Formatting happens only at the UI edge.
- **Dates as `LocalDate` epoch days** stored as `Long`.
- **Period model.** `Period(PeriodMode, anchor)` where mode is Day, Month or Year; used by Home, History and Insights so all three agree.
- **Overall budget** is a budget row with `categoryId == 0`.
- **Offline by construction.** No `INTERNET` permission, no network libraries.
- **Single Activity** (`MainActivity`) hosting Compose; handles relock on `onStop` and the `FLAG_SECURE` toggle.

## Frontend / backend

There is no backend. "Backend" concerns (persistence, calculation, security) all run on-device in the layers above.

## Authentication

Local app-lock only. See [SECURITY.md](../SECURITY.md) and the PIN section of [CONFIGURATION.md](CONFIGURATION.md).

## Testing

JVM unit tests in `app/src/test/java/com/montra/app/`: `FinanceCalcTest` and `PeriodAndSecurityTest` (23 tests in total at time of writing). UI and database behaviour are not covered by automated tests.
