# InfoDeck

Offline Android card and document manager. Each card holds named values (policy numbers,
registration numbers, passwords you choose to keep), dates with reminders, tags and attached
files — grouped into categories, searchable across every field, shareable as text, QR or files.

| | |
|---|---|
| Current version | see [`VERSION`](VERSION) — released builds are under **Releases** |
| Platform | Android 8.0+ (minSdk 26, targetSdk 34) |
| Network | none — the app holds no INTERNET permission |
| Package | `com.krishna.infodeck` |

## Repository layout

| Folder | What lives there |
|---|---|
| [`android/`](android) | The InfoDeck Android app — Kotlin, Jetpack Compose, Room |
| [`windows/`](windows) | Reserved — InfoDeck has no Windows build; the workflow skips it |
| [`macOS/`](macOS) | Reserved — InfoDeck has no macOS build; the workflow skips it |
| [`common/scripts/`](common/scripts) | Version rules, packaging, secret scan, signing check — used by CI |
| [`common/src/`](common/src) | Reserved for code shared across platforms (none yet) |
| [`docs/`](docs) | Requirements, architecture, versioning, release process, changelog, backlog, designs |
| [`.github/workflows/build.yml`](.github/workflows/build.yml) | The one build workflow for every platform |

## Download

Every merge to `main` publishes a release with **`InfoDeck_<version>.zip`**; unzip it and
install `Android/InfoDeck_<version>.apk`. Pull requests publish **beta** pre-releases
(`InfoDeck_<version>-beta.<n>.zip`). See [docs/release-process.md](docs/release-process.md).

## Build locally

```bash
cd android
./gradlew :app:testReleaseUnitTest :app:assembleRelease
```

Without the signing environment variables the release APK is built **unsigned**. To sign,
set `INFODECK_KEYSTORE`, `INFODECK_STOREPASS` and `INFODECK_KEYPASS` — never commit the
keystore or its passwords. Details in [docs/release-process.md](docs/release-process.md).

## Data and privacy

The app does not request internet access, and automatic Android cloud/device-transfer backups
are disabled. Export is user initiated. **Current JSON export does not include attached
documents**; keep separate copies of your original PDFs and Word documents until an
attachment-inclusive encrypted backup is available. JSON exports contain readable values;
store them securely.

## Working on this repo

Read [CONTRIBUTING.md](CONTRIBUTING.md) — PR naming (`cld-` / `cgpt-`), the release label
every PR needs, and the version rules.

All rights reserved. No licence is granted to copy, modify or redistribute this code.
