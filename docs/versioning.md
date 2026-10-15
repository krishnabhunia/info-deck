# Versioning — strict x.y.z

Source: [versions_management.md](standards/versions_management.md). Implementation: [version.py](../common/scripts/version.py).

**Every new version must contain exactly three non-zero-padded integer components:** `x.y.z`, including `.0` for a feature release. These are not decimal numbers: `1.9.0` precedes `1.10.0`.

| Component | Purpose | Release label | Example (from 1.11.0) |
|---|---|---|---|
| x | Major | `release:major` (Krishna only) | 2.1.0 |
| y | Minor feature | `release:feature` | 1.12.0 |
| z | Bug or error fix | `release:fix` | 1.11.1 |
| unchanged | Docs/CI only | `release:none` | 1.11.0 |

Major and feature changes reset z to zero. The first minor after a major is 1, following the project's existing numbering convention. A mixed feature+fix release is a feature release.

## Automatic version management

The single GitHub Actions workflow computes the candidate version from the current `main` version and exactly one `release:*` PR label, and writes it to the PR branch's `VERSION` file using a dedicated GitHub App token. The app must be configured once by the repository owner; if a bump is needed and it is missing, CI fails instead of pushing with `GITHUB_TOKEN` and causing an approval-required workflow. See [ci-app-token.md](ci-app-token.md). Do not edit `VERSION` manually.

- Beta PR builds use `<version>-beta.<run>` for tags, release names, APK versionName and archive names; for example `v1.12.0-beta.19` and `InfoDeck_1.12.0-beta.19.zip`.
- A merged feature/fix PR is released on `main` as `v1.12.0` with `InfoDeck_1.12.0.zip` and `Android/InfoDeck_1.12.0.apk` when signing secrets are present.
- `release:none` leaves the version unchanged and does not publish a new release.
- Android `versionCode` is `x × 1,000,000 + y × 1,000 + z`; `1.12.0` → `1012000`. Gradle reads `VERSION` and uses the workflow's beta `versionNameOverride` when supplied.
- The installed application's Settings → About Me shows `BuildConfig.VERSION_NAME`.

## One-time migration from historical x.y

The main branch currently has the historical two-part `1.11`. For this one-time transition, only `version.py next` can accept an old two-part *input*, producing `1.12.0` with the feature label. The `check` and `code` commands and Gradle reject two-part numbers. Historical tags are not rewritten.

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
