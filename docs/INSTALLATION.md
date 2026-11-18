# Installation

Montra has no published releases yet, so you build the APK yourself.

## Requirements

- JDK 17
- Android SDK with platform **35** and matching build-tools (Android Studio installs these)
- An Android 8.0+ (API 26+) device or emulator
- Internet access on the build machine the first time (Gradle and Maven dependencies)

## Build a debug APK

```bash
git clone <YOUR-REPOSITORY-URL>
cd Montra
echo "sdk.dir=/path/to/Android/Sdk" > local.properties   # skip if Android Studio created it
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Install on a device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or copy the APK to the phone and open it (you must allow installs from that source). On first launch you are asked to create a 6-digit PIN.

## Build with Android Studio

1. *File → Open* and choose the project folder.
2. Let Gradle sync finish.
3. Select the `app` configuration and press **Run**.

## Release (signed) build

See [DEPLOYMENT.md](DEPLOYMENT.md).

## Upgrading from "CHUTHIYA"

The app was previously named CHUTHIYA with package `com.chuthiya.expensetracker`. The new package `com.montra.app` installs as a **separate app**; data is not shared. To migrate: in the old app export a JSON backup, then restore it in Montra (Settings → *Backup & data* → *Restore from backup*). Backups tagged `CHUTHIYA` are accepted.
