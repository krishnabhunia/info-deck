# Architecture

Single-module Android app, `android/app`, package `com.krishna.infodeck`.

| | |
|---|---|
| Language / UI | Kotlin 1.9.24, Jetpack Compose (BOM 2024.06), Material 3 |
| Build | AGP 8.4.2, Gradle 8.7 (wrapper), JDK 17 bytecode, built on JDK 21 |
| Storage | Room 2.6.1 (schema 2), DataStore Preferences, app-private files |
| Background | AlarmManager exact alarms, BroadcastReceivers, WorkManager daily sweep |
| Other | kotlinx-serialization (backup), ZXing core (QR), AndroidX Biometric |

## Layers

```
ui/        Compose screens + AppViewModel        ── reads StateFlows, sends intents
  │
core/      Pure rules — no Android types         ── all unit-tested
  │
data/      Room DAO, Repo, Prefs, FileStore       ── the only place that touches storage
  │
alarm/ work/ backup/                              ── system integration
```

`core/` is deliberately free of Android classes so every rule runs in plain JUnit.

## Packages

| Package | Files | Responsibility |
|---|---|---|
| `core` | `ViewLogic` | grouping, sorting, filtering, the `CardRow` model |
| | `SearchRules` | search index, AND matching, ranking, row hint, missing-word report |
| | `RingCalculator` | scheduler maths — next ring, clamping |
| | `ShareText`, `FileShare`, `QrRules` | share text, send plans per target, MIME, QR capacity |
| | `Density`, `Hues`, `UiScale`, `UiFriendly`, `UiNav`, `UndoStack`, `Fmt` | presentation rules |
| | `Logger` | `Logger.e` / `Logger.guard` — every failure lands in the Error Logs table |
| `data` | `Entities`, `Dao`, `AppDatabase` | Room schema |
| | `Repo` | builds `CardRow`s (including each card's search index), transactions |
| | `Prefs` | DataStore settings |
| | `FileStore` | attachments stored under a UUID name; display name kept separately |
| `ui` | `HomeScreen`, `CardViewScreen`, `CardEditorScreen`, `UpcomingScreen`, `SettingsScreen`, … | screens |
| | `ShareUi` | picker, send-with sheet, `ShareFiles` exact-name copies, follow-up file send |
| | `AppViewModel` | combines repo + prefs + search into `HomeState` |
| `alarm` | `AlarmScheduler`, `Receivers`, `Notifications` | plan / fire / re-plan rings |
| `work` | `SweepWorker` | 00:05 safety re-plan |
| `backup` | `BackupIo` | JSON export / import (Replace or Merge) |

## Database — schema 2

| Table | Holds |
|---|---|
| `categories` | name, colour, order, built-in flag |
| `cards` | label, category, timestamps, manual order |
| `data_vars` | card, name, value, order |
| `date_vars` | card, name, epoch day, key-date flag |
| `schedulers` | date var, rule, ring time, state, last fired |
| `ring_log` | fired rings (capped at 500) |
| `error_log` | `Logger.e` entries |
| `tags`, `card_tags` | tags and the many-to-many link |
| `files` | card, display name, stored UUID name, MIME, size |

Schema JSON lives in `android/app/schemas/` and is checked into git so migrations are reviewable.

## Key flows

**Search** — `Repo` builds a `searchFields` list and one folded `searchIndex` per card when rows load.
`AppViewModel` debounces the query 150 ms, filters with `SearchRules.matches`, ranks with
`SearchRules.rank`, and attaches a `SearchHit` per row for the highlighted meta line.

**Sharing files** — attachments live as `files/cards/<card>/<uuid>.<ext>`. Before any share,
`ShareFiles.prepare` copies them to `cache/share/<display name>` (exposed through the FileProvider
`cache-path`) so receivers show the real name; the folder is swept at app start. Chat apps get the
text first and the files as a follow-up send; mail gets both in one intent.

**Reminders** — `AlarmScheduler` arms one exact alarm for the earliest due ring; the receiver fires it,
logs it, and re-plans. Boot, update and clock changes re-plan; `SweepWorker` re-plans daily.

## Permissions

`POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`,
`WAKE_LOCK`, `USE_BIOMETRIC`. **No `INTERNET`.**

## Tests

14 JUnit classes, 278 tests as of 1.9 — run with `./gradlew :app:testReleaseUnitTest`.
