# Release process

Everything below happens in [`.github/workflows/build.yml`](../.github/workflows/build.yml) — the one
workflow for Windows, macOS and Android. For automatic PR version commits without GitHub's bot-triggered approval prompts, configure the separate [repository-scoped GitHub App](ci-app-token.md). Its credentials are **not** APK signing keys.

## 1 · One-time setup — only Krishna can do this

> ### ⚑ Add three repository secrets
> GitHub → **info-deck** → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**
>
> | Name | Value |
> |---|---|
> | `INFODECK_KEYSTORE_B64` | the release keystore, base64-encoded (commands below) |
> | `INFODECK_STOREPASS` | the keystore password |
> | `INFODECK_KEYPASS` | the key password |
>
> The key alias defaults to `infodeck`. If the original key has another alias, add the
> Actions **variable** `INFODECK_KEY_ALIAS` with that alias. Do not generate a replacement key.
>
> Verify the original key locally (the command prompts for the password):
> ```powershell
> keytool -list -v -keystore "infodeck.keystore" -alias "infodeck"
> ```
> Substitute the original alias if different. Its SHA-1 must match the fingerprint below.
> Also verify an exported copy of the installed v1.9 APK with
> `apksigner verify --print-certs installed-v1.9.apk`; the fingerprint below is the repository's
> recorded expectation, not a fresh measurement of the phone. Keep keys/passwords out of commits,
> PR comments and chat. The GitHub App private key for version automation is a separate credential.
>
> **Windows PowerShell** (in the folder holding `infodeck.keystore`):
> ```powershell
> [Convert]::ToBase64String([IO.File]::ReadAllBytes("infodeck.keystore")) | Set-Clipboard
> ```
> **macOS**: `base64 -i infodeck.keystore | pbcopy` · **Linux**: `base64 -w0 infodeck.keystore`
>
> Use the **same** keystore that signed 1.1–1.9 (certificate SHA-1
> `82:50:BD:7B:80:41:21:D7:7C:91:52:FA:3C:6F:C7:6B:19:DD:14:9A`). A different key produces an APK
> Android refuses to install over the existing app — the workflow checks this and fails if it differs.
>
> Unit tests run without signing secrets, then the Android job **fails** if any required signing
> secret is missing. No intermediate APK, normal ZIP or release is uploaded. Invalid keystores,
> passwords, aliases, signatures or certificates also fail before upload. After adding the secrets,
> re-run this PR's checks to validate the fix; after review and merge, run the latest `main` workflow.
> Re-running an older workflow uses its older definition and can still produce unsigned artifacts.
>
> This card stays here until Krishna confirms it is done.

## 2 · What happens on a pull request

| Step | Job | Result |
|---|---|---|
| 1 | prepare | secret scan; release labels created if missing; platforms detected |
| 2 | prepare | **exactly one** `release:*` label required — the run fails otherwise |
| 3 | prepare | next version computed from `main`'s `VERSION` + the label, written back to the PR branch with the dedicated GitHub App installation token; if this token is not configured and a bump is needed, Prepare fails explicitly instead of creating an approval-required `GITHUB_TOKEN` run |
| 4 | windows / macos | skipped — no source in those folders |
| 5 | android | unit tests → require all signing secrets → signed release APK named `<next>-beta.<run>` → mandatory signature, original certificate, package ID, versionName/versionCode and permission checks → upload |
| 6 | package | one artifact `InfoDeck_<next>-beta.<run>.zip` (where `<next>` has three parts, e.g. `1.12.0`); GitHub **pre-release** `v<next>-beta.<run>` |

## 3 · What happens on merge to `main`

Same jobs, with the strict three-part version taken straight from `VERSION`. If tag `v<version>` does not exist yet,
the workflow publishes the **release** `v<version>` with `InfoDeck_<version>.zip`. A `release:none`
merge leaves `VERSION` unchanged, the tag already exists, so nothing is published.

## 4 · The artifact

```
InfoDeck_1.12.0.zip
└── Android/
    └── InfoDeck_1.12.0.apk
```

`portable/`, `windows-x64/` and `macOS/` appear only when those platforms have builds — per
[standards/workflows.md](standards/workflows.md), code not found in the source is skipped. The Actions
**Artifacts** box holds this one file; intermediate hand-offs between jobs are deleted.

Only verified Android APKs may enter this ZIP. The package job requires the Android job's
post-verification result and verifies the downloaded APK again before staging it. An unsigned APK
must never be renamed to the normal `InfoDeck_<version>.apk` download name. Failed signing produces
zero downloadable artifacts. Historical unsigned Actions artifacts remain unsuitable for installation;
this change does not delete old runs or replace their downloads.

## 5 · Release PR checklist (human steps)

| SrNo. | Step |
|---|---|
| 1 | Stamp `RELEASE_DATE` in `android/app/src/main/java/com/krishna/infodeck/ui/SettingsScreen.kt` to the merge day |
| 2 | Add the release section to `docs/CHANGELOG.md` and close the items in `docs/BACKLOG.md` |
| 3 | Apply one `release:*` label |
| 4 | Install the beta from the PR's pre-release and walk the device checklist |
| 5 | Merge — the release publishes itself |

## 6 · Local signed build

```bash
export INFODECK_KEYSTORE=/path/to/infodeck.keystore INFODECK_STOREPASS=… INFODECK_KEYPASS=…
# If needed: export INFODECK_KEY_ALIAS=the-original-alias
cd android && ./gradlew :app:testReleaseUnitTest :app:assembleRelease
```

The keystore and passwords stay outside the repository. `.gitignore` blocks keystore files and
`common/scripts/secret_scan.sh` fails CI if one is ever committed.

## 7 · Updates inside Android InfoDeck

Settings → **Diagnostics and About** → **Check for updates** queries the
[GitHub Releases](https://github.com/krishnabhunia/info-deck/releases) API.
Only stable releases are shown by default. **Include beta updates** is disabled
by default and opts in to signed PR pre-releases.

The app checks on startup when its last successful check is at least 24 hours
old and schedules network-constrained checks every 24 hours through WorkManager.
The Settings button forces an immediate recheck. Available updates are shown
as **Update to vX.Y.Z** or **Update to vX.Y.Z-beta.N**.

The release workflow still publishes a single ZIP archive.
The app downloads InfoDeck_<version>.zip and extracts only the APK from
Android/InfoDeck_<version>.apk into private cache. It then starts Android's
package installer. Android requires the user's explicit approval and may
request permission to install apps from InfoDeck. Silent installation is
not possible for a normal Android application. The platform verifies that
the APK is signed by the same certificate as the installed app.
