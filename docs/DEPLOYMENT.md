# Deployment and releases

Montra is a mobile app; "deployment" means producing a signed APK and distributing it. **No automatic production deployment is configured.** Publishing anywhere (GitHub Releases, Play Store, F-Droid) is a manual decision by the maintainer.

## CI workflows

| Workflow | Trigger | What it does |
|---|---|---|
| `.github/workflows/ci.yml` | push / pull request to `main`, manual | Unit tests, debug build, lint; uploads the debug APK as a workflow artifact. |
| `.github/workflows/release.yml` | **manual only** (`workflow_dispatch`) | Builds a signed release APK and uploads it as a workflow artifact. Does **not** create a GitHub Release or publish anything. |

Neither workflow has been run by the author of these files (no repository access during preparation), so verify the first run yourself.

## Signing in CI

Add repository secrets (*Settings → Secrets and variables → Actions*): `KEYSTORE_BASE64` (base64 of your `.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Without them, `release.yml` signs with a temporary key; such an APK cannot update an app installed with a different key.

```bash
base64 -w0 montra-release.jks   # Linux; copy output into the secret
```

## Local release build

```bash
export MONTRA_KEYSTORE=/path/to/montra-release.jks
export MONTRA_STORE_PASSWORD=...
export MONTRA_KEY_PASSWORD=...
./gradlew assembleRelease
```

Verify: `apksigner verify --print-certs app/build/outputs/apk/release/*.apk`.

## Publishing a GitHub Release (manual, when you decide)

1. Update `versionCode` / `versionName` in `app/build.gradle.kts` and `CHANGELOG.md`.
2. Tag: `git tag -a v1.0.0 -m "Montra 1.0.0"` (only after you approve).
3. Create the release on GitHub and attach the signed APK.

Suggested naming: tags `vMAJOR.MINOR.PATCH`, release title `Montra MAJOR.MINOR.PATCH`.

## Before any public release

- Run the unit tests and test on a real device.
- Confirm a license is chosen and `LICENSE` exists.
- Confirm Code of Conduct contact details are filled in.
