# Versioning — strict x.y.z

Source: [versions_management.md](standards/versions_management.md). Implementation: [version.py](../common/scripts/version.py).

**Every new version must contain exactly three non-zero-padded integer components:** `x.y.z`, including `.0` for a feature release. These are not decimal numbers: `1.9.0` precedes `1.10.0`.

| Component | Purpose | Release label | Example (from 1.11.0) |
|---|---|---|---|
| x | Major | `release:major` (Krishna only) | 2.0.0 |
| y | Minor feature | `release:feature` | 1.12.0 |
| z | Bug or error fix | `release:fix` | 1.11.1 |
| unchanged | Docs/CI only | `release:none` | 1.11.0 |

A major release resets y and z to zero (1.15.0 → 2.0.0, decided 16-Oct-2026); a feature release resets z to zero. The first feature after a major is x.1.0. A mixed feature+fix release is a feature release.

## Automatic version management

The single GitHub Actions workflow computes the candidate version from the current `main` version and exactly one `release:*` PR label, and writes it to the PR branch's `VERSION` file using a dedicated GitHub App token. The app must be configured once by the repository owner; if a bump is needed and it is missing, CI fails instead of pushing with `GITHUB_TOKEN` and causing an approval-required workflow. See [ci-app-token.md](ci-app-token.md). Do not edit `VERSION` manually.

- Beta PR builds use `<version>-beta.<run>` for tags, release names, APK versionName and archive names; for example `v1.12.0-beta.19` and `InfoDeck_1.12.0-beta.19.zip`.
- A merged feature/fix PR is released on `main` as `v1.12.0` with `InfoDeck_1.12.0.zip` and `Android/InfoDeck_1.12.0.apk` when signing secrets are present.
- `release:none` leaves the version unchanged and does not publish a new release.
- Android `versionCode` is `x × 1,000,000 + y × 1,000 + z`; `1.12.0` → `1012000`. Gradle reads `VERSION` and uses the workflow's beta `versionNameOverride` when supplied.
- **Beta versionCode (N65):** a PR beta carries the versionCode of `main`'s VERSION — the stable it is built on — passed as `-PversionCodeOverride`. Example: `1.16.0-beta.7` built on `1.15.0` has versionCode `1015000`. Android accepts an equal versionCode, so the stable can be reinstalled over the beta with data kept (Settings → Return to stable). Gradle refuses an override above the VERSION code; `verify_apk.sh` checks the base code.
- The installed application's Settings → About Me shows `BuildConfig.VERSION_NAME`.

## One-time migration from historical x.y

The main branch currently has the historical two-part `1.11`. For this one-time transition, only `version.py next` can accept an old two-part *input*, producing `1.12.0` with the feature label. The `check` and `code` commands and Gradle reject two-part numbers. Historical tags are not rewritten.

## Request a patch PR from GitHub Actions

The single `Build` workflow supports **Run workflow → main → action:
patch-pr**. It reads `main:VERSION` at dispatch time, calculates
`x.y.(z+1)`, and opens an unmerged, labelled `release:fix` PR changing
only `VERSION`. A separate `build` choice keeps existing manual build
behaviour. It requires the scoped App's Pull requests permission; see
[ci-app-token.md](ci-app-token.md). It does not manually release or
circumvent signing, beta checks, or branch protections.

## Checks

```bash
python3 common/scripts/version.py selftest
python3 common/scripts/version.py next 1.11 feature
python3 common/scripts/version.py next 1.12.0 fix
python3 common/scripts/version.py code 1.12.0
python3 common/scripts/version.py check 1.12.0
python3 common/scripts/verify_standards.py --version 1.12.0
```

See [release-process.md](release-process.md) for signing, artifacts and release constraints.
