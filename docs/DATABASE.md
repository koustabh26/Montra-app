# Database

Room 2.6.1 over SQLite. File: `montra.db`. Schema version **1**. `exportSchema = false`. There are **no migrations** yet.

All amounts are `Long` **paise** (1 ₹ = 100 paise). Dates are `LocalDate` epoch days (`Long`).

## Tables

### `categories`
| Column | Type | Notes |
|---|---|---|
| id | Long PK, auto | |
| name | String | |
| emoji | String | |
| color | Long | ARGB |
| type | String | `EXPENSE` or `INCOME` |
| isDefault | Boolean | |
| sortOrder | Int | |

Seeded with 28 expense and 10 income defaults (`DefaultCategories.kt`); fallbacks "Miscellaneous" / "Other Income".

### `transactions`
| Column | Type | Notes |
|---|---|---|
| id | Long PK, auto | |
| amountPaise | Long | |
| type | String | `EXPENSE` / `INCOME` |
| categoryId | Long | references a category by id (no declared foreign key) |
| note | String | |
| date | Long | epoch day |
| paymentMethod | String | Cash, UPI, Credit Card, Debit Card, Net Banking, Wallet |
| createdAt | Long | epoch millis |

Indices: `categoryId`, `date`.

### `budgets`
| Column | Type | Notes |
|---|---|---|
| id | Long PK, auto | |
| categoryId | Long | **0 = overall monthly budget**; unique index, so one budget per category |
| limitPaise | Long | |

### `goals`
`id`, `name`, `emoji`, `color`, `targetPaise`, `savedPaise`, `deadline` (nullable epoch day), `createdAt` (epoch millis).

## Backup format (JSON)

Top-level keys: `app` (`"Montra"`; legacy `"CHUTHIYA"` accepted on restore), `version`, `exportedAt`, `categories`, `transactions`, `budgets`, `goals`. Restore validates before replacing data. Backups are **unencrypted**.

## CSV export

Transaction export; cells beginning with `=`, `+`, `-` or `@` are prefixed with `'` against spreadsheet formula injection.

## Migrations

Before changing the schema: bump `version`, write a `Migration`, update `BackupManager`, and add a migration test. Shipping a schema change without a migration will crash existing installs or require destructive reset.
