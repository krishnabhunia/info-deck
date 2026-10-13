# In-app update checker — requirements (PROPOSED, not built)

Status: **queued as W8, design-first.** Nothing here exists in the app yet. An HTML phone-frame design
comes first; the code is built only on Krishna's explicit *release* trigger.

Source: [standards/versions_management.md](../standards/versions_management.md) — Android section.

## Requirements

| ID | Requirement |
|---|---|
| UPD-1 | Settings ▸ Updates holds: automatic daily check (on by default), "Include beta versions" (off by default), "Check now", last-checked time and current version |
| UPD-2 | The check reads GitHub Releases of `krishnabhunia/info-deck`; stable channel = latest non-pre-release, beta channel = newest of either |
| UPD-3 | A check also runs at app start when the last one is older than 24 h |
| UPD-4 | A newer version is decided by versionCode (`x·1,000,000 + y·1,000 + z`), never by string comparison |
| UPD-5 | When newer: notification + a card in Settings "Update to 1.10" with the release notes |
| UPD-6 | Download in the background over Wi-Fi by default; mobile data only if allowed |
| UPD-7 | The downloaded APK's signing certificate must match the installed app's before install is offered |
| UPD-8 | Install goes through Android's PackageInstaller — **one system confirmation tap is unavoidable** for a sideloaded app |
| UPD-9 | Every failure (no network, rate-limited, bad file, certificate mismatch) is logged and shown honestly |

## Decisions already taken (13-Oct-2026)

| SrNo. | Decision |
|---|---|
| 1 | Add `INTERNET`, used **only** for the GitHub release check and download, with an off switch — this ends NFR-1 "no network" for that single purpose |
| 2 | "Install silently in the background" is not possible on Android without device-owner rights; accepted as *download silently, one tap to install* |
| 3 | Requires `REQUEST_INSTALL_PACKAGES`; Android asks the user once to allow "Install unknown apps" for InfoDeck |

## Open points for the design round

| SrNo. | Question |
|---|---|
| 1 | Release asset to download: the APK attached directly to the release, or unpack it from the zip |
| 2 | Wi-Fi-only default, or any network |
| 3 | Keep the last downloaded APK for re-install, or delete after install |
