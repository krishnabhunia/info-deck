# Versioning

Source of the rules: [standards/versions_management.md](standards/versions_management.md) and the
project-wide versioning standard. Implementation: [`common/scripts/version.py`](../common/scripts/version.py)
— the only place the arithmetic lives; Gradle and the workflow both defer to it or to `VERSION`.

## Format

`x.y.z` of plain integers — **not a decimal**. `1.9 → 1.10 → 1.11`, never `2.0` by rollover, never
zero-padded (`1.09` is rejected).

| Part | Counts | Bumped by | Resets |
|---|---|---|---|
| x | major versions | label `release:major` — **Krishna only** | — |
| y | feature releases within x | label `release:feature` | to 1 when x bumps |
| z | bug-fix-only releases within y | label `release:fix` | dropped when y bumps |

* `z` is **omitted** until the first fix within a `y`: `1.10`, then `1.10.1`, `1.10.2`.
* A release that mixes fixes with **any** feature is a feature release: `y + 1`, `z` dropped.
* A major release starts at `x.1`, the same way a new app starts at `1.1`.
* `release:none` (docs, CI, tooling) leaves the version alone and publishes nothing.

## versionCode

```
versionCode = x × 1,000,000 + y × 1,000 + z         1.9 → 1009000     1.10.2 → 1010002
```

Computed by Gradle from `VERSION` at build time (`android/app/build.gradle.kts`), never typed.

## Where the number lives

| Carrier | How it is set |
|---|---|
| `VERSION` (repo root) | written by the workflow on the PR branch from the PR's label |
| `versionName` / `versionCode` | read from `VERSION` by Gradle |
| Beta `versionName` | `<next>-beta.<run>` passed by the workflow (`-PversionNameOverride`) |
| Git tag / GitHub release | `v<version>` on merge, `v<next>-beta.<run>` on PRs |
| Artifact | `InfoDeck_<version>.zip` → `Android/InfoDeck_<version>.apk` |
| About Me footer | `BuildConfig.VERSION_NAME` — automatic |
| About Me release date | `RELEASE_DATE` in `SettingsScreen.kt` — stamped by hand in the release PR (see release-process.md) |

## Worked examples

| Current | Label | Next |
|---|---|---|
| 1.9 | release:feature | 1.10 |
| 1.10 | release:fix | 1.10.1 |
| 1.10.1 | release:fix | 1.10.2 |
| 1.10.2 | release:feature | 1.11 |
| 1.11 | release:major | 2.1 |
| 1.11 | release:none | 1.11 (nothing published) |

Run `python3 common/scripts/version.py next 1.10 fix` to check any case; `selftest` runs the table above.
