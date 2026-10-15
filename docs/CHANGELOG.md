# Changelog

## Unreleased — 2.0 complete redesign: three themes (N64, design 1.15.0.D1)
- **Aurora Pop** is the new default light theme, **Neon Night** the dark theme and **Sunrise Wallet** a custom theme you can pick; System follows the phone (Aurora by day, Neon at night). Saved 1.x theme choices keep their meaning.
- Every screen gets a role-coloured header (status bar matched), a colourful Home hero with search and stats, themed section and card rows (Sunrise draws each card as a gradient wallet card), a four-tab bottom bar with Tags, and a category-coloured card hero with a big countdown.
- Settings: colour tile per group, visual theme picker; Notifications and Exact alarms moved into the Reminders group; duplicate loose Manage tags row removed.
- Bundled OFL fonts (Poppins, DM Sans, Fredoka, Nunito — Latin subsets, ≈ 500 KB) with licence texts.
- New unit tests: `Release20Test` (20 cases).


## Unreleased — N63 · Restore protected-extension Design A (15-Oct-2026)
- Restore PR #3's approved two-dialog file rename with a locked extension, a separate extension editor, and a mandatory unchecked acknowledgement box before any extension change (including case-only changes).
- Preserve PR #8's 80-character filename limit, typed-extension suggestion, common extension choices, real file-type mismatch warning, failure handling, and 5-second Undo. Storage bytes and MIME remain unchanged.
- Add regression tests for case-only and normal renames; the repository-level guard remains mandatory.

## Unreleased — one-click patch release PR request (CI-only, release:none)
- Add **Build → Run workflow → main → action: patch-pr**, deriving x.y.(z+1)
  from the current main VERSION via the existing strict version rules.
- Open a labelled `cgpt-` patch release PR; never directly merge, publish,
  change main, or bypass the existing beta/signed-APK gates.
- Add safe duplicate/stale-base handling, regression tests, design and
  GitHub App least-privilege setup documentation.

## Unreleased — never ship an unsigned APK (CI-only, fixes "package appears to be invalid")
- **B15** APKs from GitHub Actions would not install ("App not installed as package appears to be invalid"): signing secrets were not set, so CI built an unsigned APK and still put it in the Actions artifact. Merged onto the signing requirement below (`restore_signing_key.sh` + `verify_apk.sh`), which already stops the Android job after the unit tests when a secret is missing.
- Class-wide gate `common/scripts/apk_signature_gate.py` (pure Python): rejects any APK without a v1/v2/v3 signature, run in the Android job before upload and again in the package job before `verify_apk.sh` and the ZIP/release; self-test and workflow-wiring check run in Prepare.
- Fork and Dependabot PRs (no secrets) pass with unit tests only and build no APK; a blank `INFODECK_KEY_ALIAS` is treated as unset (`infodeck`) in `build.gradle.kts`.
- Troubleshooting table for "package appears to be invalid" added as section 8 of `docs/release-process.md`.

## Unreleased — require original Android signing before downloads (CI-only)
- Parse both current and legacy apksigner certificate output correctly; reject missing or malformed fingerprints instead of mistaking the certificate label for its SHA-1.
- Fail the Android job after unit tests when any signing credential is missing, instead of uploading an unsigned APK under the normal download name.
- Verify APK signatures, the original certificate, package ID, version code/name and update permissions before upload and again before ZIP packaging; derive signing success from verification.
- Add signing regression tests and document original-key secrets, optional alias and verification against the installed v1.9 APK. App code and `VERSION` are unchanged (`release:none`).

## Unreleased — GitHub App token for automated PR version updates (CI-only)
- Use an ephemeral, repository-scoped GitHub App token instead of the workflow's `GITHUB_TOKEN` to commit version bumps to PR branches, avoiding GitHub's approval-required bot-triggered CI reruns.
- Fail explicitly if version changes require an unconfigured App; no automatic approval of untrusted PR workflows and no change to signing/release rules.
- Add a pure CI wiring regression check and GitHub App setup guide (`docs/ci-app-token.md`).

## Unreleased — uploaded Markdown standards compliance (14-Oct-2026)
- Enforce three-part `x.y.z` on future stable/beta versions and Android Gradle builds; add CI tests for required directories, single workflow and exact ZIP layout.
- Audit all three supplied standards in `docs/compliance/2026-10-14-uploaded-md-audit.md`. Android automatic GitHub updates and optional beta enrollment remain unimplemented and are tracked for a separate approved design.

## Unreleased — GitHub Actions runtime/runner compatibility (CI-only)
- Migrate checkout, Java, Gradle and artifact actions to Node.js 24-compatible releases, avoiding the Node.js 20 deprecation warning.
- Pin Linux build jobs to `ubuntu-24.04` so the announced `ubuntu-latest` migration to Ubuntu 26.04 does not silently change the build environment.
- Preserve existing release labels, Android Gradle build, signing, artifacts and versioning. No application features or version bump.

## Unreleased · version assigned by PR release workflow (`release:feature` → 1.13.0)
- **N62** Design 1.9.D1 — rename keeps the extension: a bottom sheet whose name box holds the name only, with the extension locked inside it; "Change extension…" is a separate step with a does-not-convert note, and a From / To confirmation follows before anything is written. Case-only changes (.PDF → .pdf) are a plain save. Undo after every rename; file rows keep the extension visible and say "Really a PDF" while the name disagrees with the real type. **Supersedes the Design A dialog (N60)**; Design A's repository guard stays as a second check
- **B13** File rename no longer cuts names at 24 characters (it reused the tag box), and the 80-character limit now shortens the name, never the extension

## Unreleased · version assigned by PR release workflow
- **N60** Approved Design A: file names can be edited with a protected extension; changing, adding, removing or altering its case requires a second confirmation and acknowledgement. Repository validation rejects unconfirmed changes; file contents and MIME remain unchanged. Includes unit tests and a device checklist.

## Unreleased — hardening PR (after 1.9)
- Guard CI against untrusted PR titles and branch names; reduce job permissions
- Turn the offline APK permission assertion into a failing check
- Disable automatic OS backup/transfer of private data; redact notification values and avoid implicit clipboard copies
- Recover overdue reminders only when they have not already fired
- Wrap card writes and relational JSON restoration in database transactions
- JSON backup schema 2: preserve tags and card-tag assignments; accept older schema 1
- Remove orphaned attachment storage on completed full reset/replace (JSON still **does not** back up files)

Versions follow [versioning.md](versioning.md). Item IDs (N = feature, B = bug) match [BACKLOG.md](BACKLOG.md).
Releases 1.1 – 1.9 were built and signed locally before this repository existed; from now on every
release is built by GitHub Actions.

## 1.9 — 10-Sep-2026 · versionCode 1009000
- **B11** Search reads every field: all data names and values, every date (as shown and ISO), file names, tags, reminder rules; AND across words, partial, accent-folded, separator-blind for codes; ranked label › data › date › file › reminder
- **B12** Search pill with focus and keyboard on open, clear button, "N results" line with Data / Dates / Files buckets, matching field highlighted on each row with a kind badge, empty state naming the missing words

## 1.8 — 10-Sep-2026 · versionCode 1008000
- **B7** Card gap from the density table: 4 dp Compact, 6 dp Comfortable / Detailed, 8 dp between sections
- **B8** Expanded row shows three data values and Open · Edit · Share
- **B9** "Shared from InfoDeck" removed entirely
- **B10** Shared files carry their exact display names instead of the internal UUID name

## 1.7 — 08-Sep-2026 · versionCode 1007000
- **B3** About Me rebuilt to the project standard, last in Settings, feedback mail pre-filled
- **B4** Share button on every file row, real MIME type
- **B5** File names never in shared text; files go as a follow-up send to chat apps, one send to mail
- **B6** Share footer made optional *(superseded by B9 in 1.8)*
- **N58** Card density — Compact · Comfortable · Detailed
- **N59** Tap to expand a row in place

## 1.6 — 20-Aug-2026 · versionCode 1006000
- **N53** White 48 dp top bar with a gradient hairline
- **N54** 58 dp labelled bottom bar
- **N55** Nine-hue colour system, light and dark
- **N56** Category glyph tiles on every row
- **N57** File sharing in the picker *(failed on device — replaced by B4/B5 in 1.7)*

## 1.5 — 20-Aug-2026 · versionCode 1005000
- **N50** Share made prominent: Edit · Copy · QR · Share row, direct WhatsApp / Gmail / SMS / Email, .txt export
- **N51** Field picker with presets, masking and live preview before every share
- **N52** QR codes per value or combined, PNG save and share

## 1.4 — 18-Aug-2026 · versionCode 1004000
- **N40** Compact chrome and filter strip · **N41** slim bottom bar · **N42** search as an icon in the bar
- **N43** Settings grouped into a one-open accordion · **N45** Cancel / Discard + Save action bar

## 1.3 — 18-Aug-2026 · versionCode 1003000
- **N36** Visual overhaul — countdown-ring cards, overdue wash, bottom navigation
- **N37** Save · Undo · Discard bar · **N38** no reset without confirmation (typed ERASE) · **N39** friendly text sizes

## 1.2 — 18-Aug-2026 · versionCode 1002000
- **N30b** Dynamic text size · **N31** auto-capitalise · **N32** flat deck · **N33** two-layer delete
- **N34** File attachments · **N35** Tags · **B1** Snooze "Tomorrow" no longer lands in the past
- Database schema 1 → 2 (additive)

## 1.1 — 18-Aug-2026 · versionCode 1001000
First release: cards, categories, data and date variables, key date, scheduler engine with exact
alarms, notifications with Done / Snooze, accordion Home with group / sort / filter / search, selection
mode, JSON backup and restore, biometric lock, Error Logs.
