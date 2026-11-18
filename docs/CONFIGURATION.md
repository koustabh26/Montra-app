# Configuration

## Runtime configuration

None. The app has no server URLs, API keys, feature flags or `.env` file, so no `.env.example` is provided.

## Build-time signing configuration (release only)

`app/build.gradle.kts` reads these environment variables:

| Variable | Purpose | Default |
|---|---|---|
| `MONTRA_KEYSTORE` | Path to the release keystore | `../keystore/montra-release.jks` |
| `MONTRA_STORE_PASSWORD` | Keystore password | empty |
| `MONTRA_KEY_ALIAS` | Key alias | `montra` |
| `MONTRA_KEY_PASSWORD` | Key password | empty |

The release build is signed **only if the keystore file exists** at `MONTRA_KEYSTORE`; otherwise Gradle produces an *unsigned* APK that Android will not install. The password variables default to empty.

Set them in your shell only; never write real values into the repository, issues or logs.

### Creating a keystore (once, locally)

```bash
keytool -genkeypair -v -keystore montra-release.jks -alias montra \
  -keyalg RSA -keysize 2048 -validity 10000
```

Back the keystore up somewhere safe. **If you lose it you cannot publish updates** to an app signed with it. `.gitignore` excludes `*.jks`, `*.keystore` and `/keystore/`.

## GitHub Actions secrets (optional)

The release workflow can sign with these repository secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Without them it produces a throw-away ephemeral-key APK that is suitable only for testing. See [DEPLOYMENT.md](DEPLOYMENT.md).

## Gradle properties

`gradle.properties` holds JVM and AndroidX flags only; it contains no secrets.

## In-app settings

Stored on device in `montra_secure` (PIN hash, salt, failure count, lockout time, screen-privacy flag). Data is in `montra.db`.
