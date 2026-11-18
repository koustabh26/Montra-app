# Security audit report

Static review of the repository. No secret values appear in this report. No credentials were rotated and no history was changed.

## Secret scan
- Pattern scan for cloud keys, GitHub tokens, private-key blocks, Google API keys and hard-coded passwords: **no matches**.
- No `*.jks`, `*.keystore`, `*.pem`, `.env*`, `local.properties` or APK files are present in the tree.
- Signing secrets are read only from environment variables / CI secrets.
- The repository was not a Git repository, so **history could not be scanned**. If you previously committed a keystore or password in the remote `Chuthiya` repo, treat those credentials as compromised and rotate them yourself.

## Checklist
| Area | Result |
|---|---|
| API keys / tokens / OAuth / payment / Firebase | None present. |
| Database credentials | None (local SQLite). |
| Hard-coded passwords | None found. |
| Debug configuration | No logging calls in app code; debug build type unminified (normal). Lint non-aborting. |
| Network | No `INTERNET` permission; no network libraries. |
| Exported components | Only `MainActivity` (launcher). |
| Backups | `allowBackup="false"`. |
| PIN storage | Not stored; PBKDF2-HMAC-SHA256 (random 16-byte salt) + Keystore HMAC. |
| Brute force | Persisted escalating lockout. |
| Data at rest | **Not encrypted** (DB, JSON, CSV). Documented limitation. |
| CSV | Formula-injection prefixing implemented. |
| Third-party code | Official AndroidX/Compose/Room/Kotlin only. Versions are pinned; Dependabot added. |
| CI | Least-privilege `contents: read`; version-tagged actions; key material deleted after the release job; no auto-publish. |

## Recommendations
1. Enable private vulnerability reporting, secret scanning and push protection on GitHub.
2. Keep the release keystore backed up outside the repository.
3. Consider pinning GitHub Actions to commit SHAs (Dependabot can maintain them).
4. Consider encrypted backups and database encryption (see `ROADMAP.md`).
5. Obtain an independent review before relying on the PIN as strong protection.
