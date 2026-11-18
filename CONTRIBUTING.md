# Contributing to Montra

Thanks for your interest! Please read the [Code of Conduct](CODE_OF_CONDUCT.md) first.

## Ways to contribute

- Report bugs or request features through the issue templates.
- Improve documentation.
- Fix bugs or implement an agreed feature.

For anything larger than a small fix, **open an issue first** so the approach can be agreed.

## Development setup

See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md). In short: JDK 17, Android SDK 35, then `./gradlew testDebugUnitTest assembleDebug`.

## Workflow

1. Fork the repository and create a branch from `main`: `fix/short-description` or `feat/short-description`.
2. Make focused changes. Keep unrelated refactors out of the same pull request.
3. Add or update unit tests for any change to money, period, security or backup logic.
4. Run `./gradlew testDebugUnitTest assembleDebug` and make sure it passes.
5. Open a pull request using the template.

## Code guidelines

- Kotlin official style; formatting per `.editorconfig`.
- Money is `Long` paise. Never use `Double`/`Float` for amounts.
- Do not add the `INTERNET` permission or any network code without a prior maintainer-approved design discussion; offline operation is a core promise.
- Do not log PINs, amounts or personal data.
- Database schema changes need a Room migration and a version bump (see [docs/DATABASE.md](docs/DATABASE.md)).

## Commit messages

Short imperative summary (≤ 72 characters), optional body explaining why. Example: `Fix budget amount field losing focus`.

## Never commit

Keystores (`*.jks`, `*.keystore`), passwords, `local.properties`, `.env` files, APK/AAB files. `.gitignore` covers these, but check your diff.

## Security issues

Do **not** file public issues for vulnerabilities. Follow [SECURITY.md](SECURITY.md).

## Licensing of contributions

A project license has not been selected yet (see README). Maintainers will state the terms for contributions once one is chosen.
