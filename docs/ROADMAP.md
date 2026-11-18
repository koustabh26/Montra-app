# Roadmap

These are ideas gathered from known gaps in the current code, **not commitments or dated promises**. The maintainer decides what is accepted.

## Near term
- Run the first end-to-end CI build and manual device test; publish a first release if satisfied.
- Add screenshots and a logo to the README.
- Choose and add a license.

## Quality
- Instrumented/UI tests for core flows (add transaction, budgets, backup/restore).
- Room migration tests; enable schema export.
- Split `AppViewModel` per feature.

## Security
- Optional biometric unlock.
- Optional encrypted backups (passphrase-based).
- Consider an encrypted database (e.g. SQLCipher), weighing the dependency cost.

## Features
- Recurring transactions.
- Per-category budget alerts.
- Additional export formats.
- Localisation of strings.
- Accessibility review (TalkBack, contrast, font scaling).
