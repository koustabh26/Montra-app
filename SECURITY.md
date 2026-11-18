# Security Policy

## Supported versions

| Version | Supported |
|---|---|
| `main` (pre-release 1.0.0) | Yes |

No tagged release exists yet.

## Reporting a vulnerability

**Please do not open a public issue.** Use GitHub's private vulnerability reporting: on the repository, go to **Security → Report a vulnerability**. (Maintainer: enable this under *Settings → Code security*.) Include the app version/commit, Android version, reproduction steps and impact.

You can expect an acknowledgement as soon as the maintainer is able; this is a volunteer project and no response-time guarantee is made.

## Security design (what to expect)

- **Offline:** the app declares no permissions, including `INTERNET`, and contains no network code.
- **PIN:** 6 digits, PBKDF2-HMAC-SHA256 with a random 16-byte salt, sealed with an Android Keystore HMAC key. Weak PINs (e.g. repeated or sequential digits) are rejected. Failed attempts trigger a persisted, escalating lockout.
- **Relock:** the app locks when it goes to the background.
- **Screen privacy:** `FLAG_SECURE` can block screenshots and recent-app previews.
- **Backups:** `allowBackup="false"`, so Android cloud/adb backup does not copy app data.
- **CSV export:** cells starting with formula characters are neutralised.

## Known limitations (not vulnerabilities, but be aware)

- The Room database is **not encrypted**. The PIN gates the UI only; a rooted device or physical extraction could read the database.
- JSON backup and CSV export files are **unencrypted**. Store them carefully.
- There is no PIN recovery.
- No biometric unlock.
- Not independently audited.

## Handling secrets

Never commit keystores, passwords or `.env` files. Release signing material belongs in local environment variables or in CI secrets only. See [docs/CONFIGURATION.md](docs/CONFIGURATION.md).
