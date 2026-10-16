# InfoDeck compliance audit — uploaded Markdown standards

Date: 14-Oct-2026. Scope: `krishnabhunia/info-deck`, compared with the three supplied Markdown files (`versions_management.md`, `workflows.md`, `github.md`). Identical rule documents already live under `docs/standards/`.

Status meanings: **Pass** = observed in the current repository; **Fixed in this PR** = code change proposed, not yet released; **Pending** = requirement not implemented; **N/A** = no application exists on that platform. A successful workflow cannot prove in-app behavior on a device.

## versions_management.md

| Requirement | Status | Evidence or gap |
|---|---|---|
| Every version has format x.y.z | **Fixed in this PR** | Previously `VERSION=1.11` and the script/Gradle accepted x.y. Updated `common/scripts/version.py` and `android/app/build.gradle.kts` require x.y.z. PR's automatic feature bump wrote `1.12.0`. The one-time `next` input still accepts legacy `1.11`; strict `check` does not. |
| x major / y minor / z fix | **Fixed in this PR** | Next-version arithmetic emits `(x+1).0.0`, `x.(y+1).0` or `x.y.(z+1)` (major changed from `(x+1).1.0` on 16-Oct-2026 at Krishna's request). |
| GitHub automatically manages version | **Pass** | `.github/workflows/build.yml` calculates based on a single release label and writes `VERSION` on the PR branch. `release:none` is excluded from publishing. |
| Check for new version after restart | **Pending** | No release-checking code exists in `MainActivity`/`InfoDeckApp`. Current Android app deliberately has no `INTERNET` permission. |
| Settings-based download and silent installation | **Pending / platform limitation** | No updater is implemented. Ordinary sideloaded Android apps cannot silently install upgrades: user confirmation via Android's system installer is required. Future implementation must use background download followed by system-confirmed installation. |
| Beta optional | **Partial** | Workflow creates prereleases when signed; there is no Android setting to opt into beta updates. |
| PR beta, merged `main` stable release | **Pass, conditional** | Workflow computes beta tag `v<version>-beta.<run>`, stable tag `v<version>` on merge, if the signing certificate/secrets allow publishing. An unsigned APK is not released. |
| Windows/macOS update button only when update found, labeled `Update to vx.y.z` | **N/A** | Both platform roots are reserved and contain no app source. Must be implemented before a Windows or macOS version is shipped. |
| Windows/macOS version visible in main window | **N/A** | No Windows/macOS app. Android's About Me already uses `BuildConfig.VERSION_NAME`. |
| Android Settings daily GitHub check with optional beta | **Pending** | `docs/requirements/update-checker.md` explicitly marks this feature **proposed**, not built. It needs a design approval, networking permission, secure APK verification, background scheduling and device testing. |

## workflows.md

| Requirement | Status | Evidence or gap |
|---|---|---|
| One GitHub Actions workflow for Windows, macOS, Android | **Pass** | Only `.github/workflows/build.yml`; it defines all three platform jobs, Prepare and Package. |
| Output artifact is ZIP, named `InfoDeck_<version>.zip` | **Pass** | Workflow package job produces a ZIP named from software and computed version; GitHub artifact name is `InfoDeck_<version>` and GitHub supplies the `.zip` download. |
| Platforms without source can be skipped | **Pass** | Detect step skips Windows and macOS when their directories only contain README placeholders. |
| Only one final file in the Actions Artifacts box | **Pass on observed run** | Successful PR #5 run 38614160762 exposed one non-expired artifact `InfoDeck_1.12.0-beta.20`. Intermediate `part-android` artifact is deleted by the packaging job. This is a runtime check, not just a source claim. |
| Internal `portable/...`.exe, `windows-x64/...`.exe, `macOS/...`.dmg and `Android/...`.apk | **Pass for current Android; other platforms conditional** | `common/scripts/package.sh` uses those exact paths when platform inputs exist. New `verify_standards.py` exercises both Android-only and all-platform fixtures. Production Windows/macOS files cannot yet be verified because apps do not exist. |

## github.md

| Requirement | Status | Evidence or gap |
|---|---|---|
| `docs`, `windows`, `macOS`, `android` are present | **Pass** | All four top-level directories are checked into the repository. |
| Common scripts/source directories or equivalent | **Pass** | `common/scripts/` and `common/src/` exist. The source standard permits renaming these paths. |
| Other files/folders allowed | **Pass** | `.github`, `README.md`, `VERSION` etc. are compatible with the standard. |
| Claude PR titles start `cld-` | **Policy documented** | `CONTRIBUTING.md` has the convention. This audit did not prove origin-specific enforcement of every historical PR title. |
| ChatGPT PR titles start `cgpt-` | **Pass for this PR** | New PR is named `cgpt-audit-uploaded-standards-and-version-format`; `CONTRIBUTING.md` documents the naming rule. |

## Implementation and validation

- This PR changes **version parser + Gradle validation**, release documentation, and adds a CI smoke check for required paths and archive layout.
- A release label of `release:feature` is needed for the change; **do not manually edit `VERSION`**. Main's historical `1.11` is intentionally left alone until merge, when the workflow writes the feature version.
- **No Android networking/update installation code is added**. Adding `INTERNET` now would violate the offline permission check without delivering the updater. The feature is tracked separately in the existing update-checker specification and backlog.
- PR #4 has been merged into `main`; its Node.js 24 and Ubuntu 24.04 workflow updates are preserved in this branch. The audit also passed the PR #5 GitHub Actions run 38614160762 before this merge reconciliation. Re-run verification after the new merge commit.
- CI validation: `python3 common/scripts/version.py selftest` and `python3 common/scripts/verify_standards.py --version <computed>`; Android release unit tests/build, certificate check, and artifact packaging remain mandatory.

## Suggested follow-up

Approve the proposed Android update Settings screen and define its release-download path. Then implement daily/background checks, opt-in beta, download validation and user-confirmed installation in a **separate feature PR**; do not claim silent Android installation.
