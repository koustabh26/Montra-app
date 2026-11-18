# GitHub presentation recommendations

Nothing below has been applied; these are suggestions for the maintainer.

## Repository name
The current remote name is `Chuthiya`, which no longer matches the app. Suggested: **`Montra`** (GitHub redirects the old URL after a rename). Renaming is a maintainer action in *Settings → General*.

## Description (suggested, ≤ 350 chars)
"Private, offline-first expense tracker for Android (Kotlin, Jetpack Compose, Room). Amounts in ₹, no account, no network permission."

## Topics (suggested)
`android`, `kotlin`, `jetpack-compose`, `room-database`, `expense-tracker`, `personal-finance`, `offline-first`, `material3`, `mvvm`, `privacy`

## Assets still needed (none exist yet — do not fabricate)
- **Logo:** `docs/assets/logo.png` (the app has an adaptive icon in `res/`, which could be exported).
- **Screenshots:** capture from a real device/emulator into `docs/assets/screenshots/` (Home, History, Budgets, Insights, Lock).
- **Social preview image:** 1280×640 px, uploaded in *Settings → Social preview*.
- **Demo GIF:** optional.

## README badges
Add only real, working badges once the repo is public, e.g. the CI status badge for `ci.yml` (`https://github.com/<owner>/<repo>/actions/workflows/ci.yml/badge.svg`) and a license badge after a license is chosen. No badges are included now because none can be verified.

## Release naming
Tag `vMAJOR.MINOR.PATCH` (first: `v1.0.0`), title `Montra 1.0.0`, notes copied from `CHANGELOG.md`, signed APK attached.

## Repository settings to enable (maintainer)
Private vulnerability reporting, Dependabot alerts, secret scanning/push protection, branch protection on `main` requiring the CI check, and optionally GitHub Discussions.
