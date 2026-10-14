# InfoDeck — PIPELINE QUEUE (BACKLOG)

### Unreleased · N60 — protected file extension rename (Design A, approved 14-Oct-2026)

| ID | Feature | Exceptions | Error logging | Tests |
|---|---|---|---|---|
| N60 | Separate editable stem and locked extension; unlock explicitly and confirm extension changes in a second dialog with acknowledgement | Validate filename/extension before save; back and cancel make no changes; repository rejects unconfirmed extension updates and guards storage errors | `Repo.renameFile` errors and rejected changes use `Logger.e` | `FileRenameRulesTest` for ordinary changes, protected changes, removal, addition, invalid characters/length; phone checks below |

Device checklist: (1) rename `policy.pdf` to `renewed.pdf` with extension locked, no second dialog; (2) tap Change extension, edit to `docx`, tap Rename, verify second warning and acknowledgement; (3) cancel or go back and verify original filename; (4) confirm, then verify label updates but original bytes and format do not change; (5) adding/removing extensions also requires confirmation; (6) invalid/overlength names cannot be saved; (7) reopen/share after change and verify no crash. No automatic conversion is implied.


### v1.2 — versionCode 1002000 — 18-Aug-2026 — SHIPPED (mixed release: y bumps, z dropped)

| SrNo. | ID | Item | Status |
|---|---|---|---|
| 1 | N30b | Dynamic text size 70–200 %, live, per-role offsets, pinch-to-zoom, spacing, weight, follow-system | shipped |
| 2 | N31 | Auto-capitalise words, with a names-only / all-fields switch | shipped |
| 3 | N32 | Group by "None" renders a flat card deck (search too) | shipped |
| 4 | N33 | Two-layer delete: Danger zone sheet everywhere, editor bar down to Cancel + Save, typed DELETE at 5+ cards | shipped |
| 5 | N34 | File variables — PDF/DOCX copied in, open/share/rename/remove, FileProvider | shipped |
| 6 | N35 | Tags — picker, chips, Group by Tag, Any/All filter state, Manage tags with merge | shipped |
| 7 | B1 | Snooze "Tomorrow" no longer lands in the past | shipped |
| 8 | — | Schema 1 → 2 additive migration (tags, card_tags, files) | shipped |

Not in 1.2, still queued: Undo snackbar after card delete (Q6), tag quick-chips row and the Tags block
inside the Filter sheet UI (the filter state and logic ship, the sheet controls do not), "Backup with
files" zip export, and the file-manifest addition to the JSON backup.

Source of truth for InfoDeck. Written atomically. Every add/modify/fix item carries its
**three companions** at queue time: (1) exception handling, (2) `Logger.e` logging,
(3) test cases (JUnit for pure logic + a written device checklist for anything only a
real device can prove).

App: **InfoDeck** · package `com.krishna.infodeck` · Android only (iOS paused project-wide).

---

## 1 · Released

### v1.1 — versionCode 1001000 — 18-Aug-2026 — FIRST RELEASE

Scope = design **D1** + design **D2**, accepted as drawn ("I accept the design 2 and add
everything to queue"). All D1 Q1–Q10 and D2 Q11–Q17 taken at the recommended option.

| SrNo. | ID | Item | Companion 1 — exceptions | Companion 2 — Logger.e | Companion 3 — tests |
|---|---|---|---|---|---|
| 1 | N1 | Card model: label + N data variables (any text ≤500 ch) + N date variables | All var writes inside one Room transaction; blank rows dropped; lengths clamped (label 60, name 40) | `Repo.saveCard` guarded → Error Logs | Room schema v1 asserted (7 tables); save/replace path exercised via backup round-trip |
| 2 | N2 | Category = the only grouping level; built-in **Uncategorised** (id 1, never deletable/renameable) | Unique index on name; `deleteCategory` refuses `builtIn` | `Repo.addCat/updCat/delCat` guards | `ViewLogicTest` category grouping + built-in always last |
| 3 | N3 | Key date **★** — one starred date per card drives expiry grouping/sorting/filtering; first date auto-starred | Falls back to first date when star missing or deleted | `Repo.rows` guard | `ViewLogicTest` missing-key-date rows sink last in both directions |
| 4 | N4 | Scheduler engine: Same day / 1–450 days / 1–60 weeks / 1–60 months / 1–20 years, Before ⇄ After, per-scheduler ring time | Out-of-range amounts **clamp**, never extrapolate; month-end and 29-Feb clamp via `java.time` | — (pure) | **38 JUnit tests** in `RingCalculatorTest` incl. 31-Mar−1m→28-Feb, 2024 leap→29-Feb, +1y→28-Feb, ring-minute coercion |
| 5 | N5 | Ring delivery: one exact alarm for the earliest due ring, re-planned after each fire | Falls back to `setAndAllowWhileIdle` when exact alarms not permitted | `Alarm.plan` logs "exact alarms not permitted", "nothing to schedule", scheduling failure | Device checklist D1–D6 below |
| 6 | N6 | Missed rings fire on next pass; never re-fires the same moment (`lastFiredAt` guard) | `goAsync()` + try/finally so the receiver always finishes | `RingReceiver` logs every failed pass | Device checklist D3 |
| 7 | N7 | Notification: high-priority, grouped, **Done** / **Snooze** actions, tap opens the card | Catches `SecurityException` when POST_NOTIFICATIONS is denied | logs "notifications disabled by user; ring N not shown" | Device checklist D1, D4 |
| 8 | N8 | Boot / package-replace / time-change / timezone-change re-plan | Whole receiver guarded | `BootReceiver` logs the triggering action every time | Device checklist D5 |
| 9 | N9 | Daily sweep at 00:05 (WorkManager) as belt-and-braces re-plan | `Result.retry()` on failure | `SweepWorker` logs failure | dex proof `infodeck_daily_sweep` |
| 10 | N10 | Home = single accordion, **one section open at a time** (switchable, default on) | Open-section key persisted in DataStore; unknown key = all collapsed | `Prefs` read/write guarded, falls back to defaults | `ViewLogicTest` section building |
| 11 | N11 | Group by: Category / Expiry month / Expiry window / Next reminder / None + section order Manual, A-Z, Nearest date | Empty sections dropped; "N categories hidden by filters" line | via `VM.home` guard | `ViewLogicTest` all four grouping modes |
| 12 | N12 | Sort by: Name / Expiry / Next reminder / Recently updated / Recently added / Manual, both directions, **missing values always last** | Total comparators, null-safe | — (pure) | `ViewLogicTest` sorts both directions + missing-last |
| 13 | N13 | Filters: categories (multi), expiry window, reminder state, content + live "Show N cards" button | Unknown persisted enum → default | `Prefs` enum decode falls back | `ViewLogicTest` every filter group incl. inclusive custom range |
| 14 | N14 | Search suspends grouping and shows one flat list | Query trimmed/lower-cased; empty = normal view | via `VM.home` guard | covered by grouping tests (GroupBy.NONE path) |
| 15 | N15 | Selection mode: long-press → Move / Reminders off / Duplicate / Delete, select-all, undo snackbar on move | Bulk ops re-plan alarms afterwards | `Repo.move/dup/del` guards | Device checklist D7 |
| 16 | N16 | Manage categories: add, rename, colour (8 fixed), reorder ↑↓, delete with **move-cards or delete-cards (typed DELETE)** | Duplicate name rejected before write; built-in protected | `Repo` category guards | `ViewLogicTest` ordering; device checklist D8 |
| 17 | N17 | Card view: copy any value, share as text (variables only, never schedulers), move, delete | Clipboard/share wrapped in `runCatching` | — | Device checklist D9 |
| 18 | N18 | Card editor with leave-guard, date picker, ★ toggle, delete-date warns about dependent schedulers | Schedulers re-pointed by local date id when dates are rewritten | `Repo.saveCard` guard | backup round-trip proves re-pointing survives export/import |
| 19 | N19 | Upcoming + History (ring log, capped 500) | Log trimmed after each insert | ring log written on every fire | dex proof; device checklist D10 |
| 20 | N20 | Settings: ring time, vibrate, snooze, permissions (notification + exact-alarm deep links), theme, single-open switch, backup/restore, app lock, Error Logs, Reset (typed RESET) | Every settings write guarded; reset keeps settings | `Prefs` write failures logged | `FmtTest` time formatting; device checklist D11 |
| 21 | N21 | JSON backup / restore via system file picker, **Replace all** or **Merge** | Bad/garbage JSON returns null, never crashes; unknown future fields ignored | `BackupIo` logs decode/export/import failures | **6 JUnit tests** `BackupRoundTripTest` incl. garbage, empty, unknown-field |
| 22 | N22 | App lock (biometric / device credential), **off by default** | If no enrolment or prompt throws → unlocks rather than locking the owner out, and logs it | `Lock` logs auth errors and fallbacks | Device checklist D12 |
| 23 | N23 | Error Logs screen — every `Logger.e` entry, share, clear | Logger never throws from its own sink | this is the sink | dex proof; visible count on Settings |
| 24 | N24 | **Sheet clearance standing rule**: every sheet reserves `max(25 % screen height, nav-bar + 24 dp)`; nav-bar measured once from the decor view; every sheet passes `windowInsets bottom = 0`; one shared EditorSheet chrome (Cancel outlined + Save filled + Delete slot, equal width, 50 dp) | Single source of clearance — no per-sheet inset reads | — | dex proof `SheetBottomSpace` ×3; device checklist D13 |
| 25 | N25 | dd-MMM-yyyy everywhere; About Me block standard and always expanded | — | — | **7 JUnit tests** `FmtTest` |

**Negative controls shipped in v1.1**

| SrNo. | Control | Result |
|---|---|---|
| 1 | No `INTERNET` permission | `aapt2 dump badging` → 0 matches. The app cannot phone home |
| 2 | Amounts beyond the allowed range | clamp to the bound, verified by test (10 000 days ≡ 450 days) |
| 3 | Corrupt backup file | `decode` returns null, UI says "Restore failed — see Error Logs" |
| 4 | Ring moment already past | scheduler shows "Past (won't ring)" and the sheet preview turns red — no silent fire |
| 5 | Built-in category | cannot be renamed, recoloured, reordered or deleted |

---

## 2 · Device checklist for v1.1 (only a real device can prove these)

| SrNo. | ID | Check |
|---|---|---|
| 1 | D1 | Settings → **Ring now** shows a heads-up notification with Done + Snooze |
| 2 | D2 | Create a card with a date = today and a Same-day scheduler at now+2 min → it rings at that minute |
| 3 | D3 | Set a ring in the past, force-stop, reopen → it fires once on the next pass, never twice |
| 4 | D4 | Snooze → notification returns after the configured snooze length; Done → status becomes Done |
| 5 | D5 | Reboot the phone → reminders still fire (BootReceiver re-plan) |
| 6 | D6 | Revoke "Alarms & reminders" → Settings shows **Not allowed**; ring still arrives, within a window |
| 7 | D7 | Long-press a card → Move 3 cards → undo snackbar appears; counts update |
| 8 | D8 | Delete a category with cards → both branches (move / typed DELETE) behave as drawn |
| 9 | D9 | Copy a value → clipboard; Share as text → schedulers absent from the shared text |
| 10 | D10 | Upcoming shows month headers; History lists fired rings with Done/Snoozed/Shown |
| 11 | D11 | Backup writes a .json through the picker; Restore Replace and Merge both land |
| 12 | D12 | App lock on → prompt on cold start; cancel → stays locked; no enrolment → opens (logged) |
| 13 | D13 | **Every** sheet (group, sort, filter, move, scheduler, category) clears the nav bar on your device |

---

## 3 · Open questions carried forward

| SrNo. | Question | My position |
|---|---|---|
| 1 | versionCode formula `x×1000000 + y×1000 + z` — still unconfirmed project-wide | applied as 1001000; one-line change if you decide otherwise, and nothing has shipped on top of it |
| 2 | Manual card order inside a category (drag) — not built; "Manual" sort currently uses insertion order | queue as N26 if you want drag-reorder |
| 3 | Snooze "Tomorrow" option is stored as −1 and currently treated as minutes | queue as B1 (bug) — see below |

---

## 4 · Queue (nothing built without an explicit trigger)

### GitHub repository set-up — 13-Oct-2026 (approved: "your defaults and recommendation")

| SrNo. | ID | Item | Status |
|---|---|---|---|
| 1 | W1 | Initial import into `krishnabhunia/info-deck` via PR `cld-initial-import`; keystore and password literals removed from the source | in PR |
| 2 | W2 | Docs: SRS, update-checker requirements, architecture, versioning, release process, changelog, design archive, rule files | in PR |
| 3 | W3 | `VERSION` file as single source; Gradle computes versionCode; PR label decides the bump | in PR |
| 4 | W4 | One workflow `build.yml` — Windows/macOS skipped without source, Android tested + built + signed, one-file artifact | in PR |
| 5 | W5 | Beta pre-release on every PR, release on merge to `main` | in PR |
| 6 | W6 | Secret scan and signing-certificate check fail the build on violation | in PR |
| 7 | W7 | `cld-` / `cgpt-` PR naming, PR template, CONTRIBUTING | in PR |
| 8 | W8 | In-app update checker (daily, optional beta) — needs INTERNET + REQUEST_INSTALL_PACKAGES | **design-first, not built** — see `docs/requirements/update-checker.md` |

Prerequisite only Krishna can do — add secrets `INFODECK_KEYSTORE_B64`, `INFODECK_STOREPASS`,
`INFODECK_KEYPASS` (step-by-step in `docs/release-process.md` §1). Stays open until he confirms.


### APPROVED 08-Sep-2026 ("add to queue") — drawn in `infodeck-1.6.D2-design.html`, nothing built

| SrNo. | ID | Item | Three companions |
|---|---|---|---|
| A | N58 | **Card density — Compact · Comfortable · Detailed.** Compact 44 dp (26 dp tile, title, bare number in a 30 dp ring), Comfortable 60 dp (34 dp tile, title, "date name + date" meta line, ring with unit — category carried by the tile, NOT text, because "date · category" truncated when rendered), Detailed 76 dp (today's 1.6 row). Chosen in the View sheet (three tiles with a sketch and the dp) and mirrored in Settings ▸ Look and feel. Default Comfortable; one setting, Upcoming and search follow. Density decides WHAT a row shows; text size decides HOW BIG — they multiply. Replaces the older "Compact / Cozy / Roomy" extra | unknown stored value falls back to Comfortable; heights from one table so no 0 dp row; `Logger.e` on unknown value; JUnit on height table x text scale for all three and the fallback |
| B | N59 | **Tap to expand.** A switch, not a fourth density: tapping a row (title, tile and ring still open the card) expands it in place — chip row, up to THREE data variables, Open · Edit · Share — one open at a time, indigo outline, 180 ms. Defaults on for Compact, off otherwise | expanded id validated against the visible list, a deleted card collapses; only three values rendered; `Logger.e` on a vanished id; JUnit on one-open rule, three-value cap, default-per-density |

If accepted these ride with B3/B4 as **1.7**, versionCode **1007000**, no schema change.

### APPROVED 08-Sep-2026 ("add to queue") — drawn in `infodeck-1.6.D3-design.html`, nothing built

| SrNo. | ID | Item | Three companions |
|---|---|---|---|
| A | B5 | **File names in the message instead of files (WhatsApp).** Two stacked causes, both verified in source: (1) `ShareText.build()` lists `Kind.FILE` in its output order, so a ticked file was written as "name: name" into the text; (2) the attach intent used `*/*` with a pinned package and silently fell back to text-only. Fix: files are NEVER text (FILE removed from the builder); files return to the picker as per-file rows; they travel as a **second send** right after the text for chat apps (real MIME per file, `ACTION_SEND`/`SEND_MULTIPLE`, pinned to the same app, offered by an indefinite "N files ready - SEND FILES" snackbar on return), while Gmail/Email get text + files in one intent. Amends B4: per-file Share pill stays, picker regains file rows, Attach/Bundle switch stays gone, masked-plus-attached warning returns | send 2 only after send 1 launched; failed send 2 announces itself and keeps files pending; missing file skipped and named; `Logger.e` on failed send 1/2, missing file, any degrade; JUnit that build() never contains a file name, the per-target plan (one send vs two), MIME per file, pending-files lifecycle; device checklist into WhatsApp and Gmail |
| B | B6 | **"Shared from InfoDeck" optional.** Preference `shareFooter`, default OFF, switch in the picker under Options and mirrored in Settings ▸ Sharing when that screen lands; exactly one line, last, after a blank line | missing preference reads false; JUnit that the footer appears only when true and always last |

### v1.7 — versionCode 1007000 — 08-Sep-2026 — SHIPPED

| SrNo. | ID | Item | Status |
|---|---|---|---|
| 1 | B3 | About Me rebuilt to ultra-compact-B: accent header strip, KB avatar, "Developer", LinkedIn/Facebook as circular buttons (glyphs "in"/"f" — no brand icons in the material set), chips Suggestion/Feedback/Bug/Error + ✉ Send, caption with the address, two-line footer live from BuildConfig; LAST in Settings, outside the accordion, never collapsible; mail pre-filled with subject `[InfoDeck 1.7] <type>` and the nine-line context block; ActivityNotFoundException guarded with a copyable address | shipped |
| 2 | B4 | Per-file green Share pill on every file row, real MIME via the existing FileStore.share | shipped |
| 3 | B5 | Files never in the text (FILE removed from the builder); two-step send for WhatsApp (text pinned, then an indefinite "N files ready - SEND FILES" snackbar firing the files with real MIME to the same app), one send with attachments for Gmail/Email, text-only for SMS; degrade announced | shipped — per-app behaviour UNVERIFIED on device |
| 4 | B6 | "Shared from InfoDeck" optional — preference `share_footer`, default off, switch in the picker under Options | shipped (Settings ▸ Sharing mirror NOT built — that screen still does not exist) |
| 5 | N58 | Density Compact 44 / Comfortable 60 / Detailed 76 dp; tiles in the View sheet with the dp; Settings ▸ Look and feel row cycles the three on tap; default Comfortable | shipped |
| 6 | N59 | Tap to expand — row tap expands in place with indigo outline, one open at a time, title/tile/ring still open the card; switch in View sheet and Settings; defaults on for Compact | shipped PARTIALLY: the expanded row shows ONE data value (HomeRow carries only the first) and Open + Edit — not three values and no Share |

Toolchain note: the container had been wiped; JDK 21, Gradle 8.7 and the Android SDK were reinstalled from scratch before this build.

### APPROVED 10-Sep-2026 ("Add to queue") — drawn in `infodeck-1.7.D1-design.html`, nothing built

| SrNo. | ID | Item | Three companions |
|---|---|---|---|
| A | B7 | **Card gap too big.** Root cause: 8 dp list spacing + 7 dp section spacer + 1 dp borders ≈ 16 dp. Fix: one gap value per density next to the row height — 6 dp at Comfortable/Detailed, 4 dp at Compact; header-to-first-card and section-to-section 8 dp. Two more cards before the fold | read from the density table; JUnit that each density yields one gap and Compact is smallest |
| B | B8 | **Share in the expanded row** (Open · Edit · Share, Share green-filled) opening the existing picker and Send-with sheets on Home; two-step file send works from Home. Also closes the 1.7 gap: row model gains `dataPreview` (first three data variables) | sheet only for a visible card, pending files scoped to Home and cleared on leaving; `Logger.e` on a share for a vanished card; JUnit on the three-value preview and pending-files lifecycle |
| C | B9 | **"Shared from InfoDeck" removed entirely** — switch, "Options" heading, preference `share_footer`, `ShareText.FOOTER` and the `footer` parameter all deleted; message ends on the last field in shares, Copy, .txt export and QR; queued Settings row dropped | JUnit that no share/copy/export/QR text ever contains the phrase |
| D | B10 | **Exact file names when sharing.** Root cause: attachments stored under `UUID.randomUUID()`, share URI pointed at the stored file. Fix: copy to `cache/share/<displayName>` just before the intent (name sanitised, extension kept, clashes suffixed " (1)"), one helper `ShareFiles.prepare()` feeding the pill, follow-up send, mail and chooser; folder emptied after the share and swept at app start; failed copy falls back to the stored file, logged | copy failure → fallback + `Logger.e`; JUnit on sanitising, extension preservation, clash suffixing; device check that WhatsApp shows the display name |

If accepted these ship as **1.8**, versionCode **1008000**, no schema change.

### v1.8 — versionCode 1008000 — 10-Sep-2026 — SHIPPED

| SrNo. | ID | Item | Status |
|---|---|---|---|
| 1 | B7 | Card gap from the density table: 4 dp Compact, 6 dp Comfortable/Detailed; section gap 8 dp | shipped |
| 2 | B8 | Expanded row shows the first THREE data variables (`CardRow.dataPreview`) and Open · Edit · Share; Share navigates to the card view with the picker auto-opened (`card/{id}?share=true`) | shipped — picker opens on the card view, not in place on Home as drawn |
| 3 | B9 | "Shared from InfoDeck" removed entirely — switch, Options heading, `share_footer` preference, `ShareText.FOOTER`, `footer` parameter; 0 dex matches | shipped |
| 4 | B10 | Exact file names: `ShareFiles.prepare()` copies to cache/share/<display name> (sanitised, clashes suffixed) before every share — pill, follow-up send, mail; FileProvider gains a `cache-path` for share/; swept at app start | shipped — receiver display UNVERIFIED on device |

Note: the first 1.8 build went out with RELEASE_DATE still 08-Sep; caught in verification and rebuilt with 10-Sep before delivery.

### APPROVED and RELEASED 10-Sep-2026 — drawn in `infodeck-1.8.D1-design.html`

| SrNo. | ID | Item | Three companions |
|---|---|---|---|
| A | B11 | **Search reads every field.** Verified in `AppViewModel`: the filter tests only label, keyDateName, firstDataName/Value, category and tags — every data variable after the first, every date VALUE, non-key date names, file names and reminder rules are invisible. Fix: one lowercase accent-folded index string per card built at load (all data names+values, each also separator-stripped, all date names + displayed dates + ISO, file names, reminder rule, tags, category); AND across words; partial hits; ranked label > data > date > file > reminder; debounced 150 ms; blank query = everything; trimmed at 100 chars | null names/values contribute nothing, index build never throws; `Logger.e` on a per-card index failure; JUnit that every field kind lands in the index, stripped forms exist, accents fold, AND across words, separator-blind codes, ordering, and the which-word-missed report |
| B | B12 | **Search field UX.** 1.8 shows Material's default filled text box with no focus/keyboard. Fix: 38 dp pill in the indigo tint inside the 48 dp bar, focus + keyboard on open, round ✕ inside the pill, Back collapses and clears, "N results for 'q'" count line with All / Data / Dates / Files bucket chips when hits span kinds, the row's meta line becomes the MATCHING field with the hit highlighted and a DATA/DATE/FILE/TAG/REMINDER badge, an empty state naming which words matched and which did not, flat list while searching (filters still apply, View badge visible); same pill on Upcoming and Settings | highest-ranked matching field chosen for the hint, label match shows the normal meta; JUnit on hint field selection and highlight range |

If accepted these ship as **1.9**, versionCode **1009000**, no schema change.

### v1.9 — versionCode 1009000 — 10-Sep-2026 — SHIPPED

| SrNo. | ID | Item | Status |
|---|---|---|---|
| 1 | B11 | Search reads every field: `core/SearchRules.kt` (fold, stripped twin, AND across words, rank, hit, missing, kindsHit); `CardRow.searchFields/searchIndex` built in Repo from label, category, all data, all dates (displayed + ISO), file names, tags, reminder rules; debounce 150 ms; query trimmed at 100 | shipped |
| 2 | B12 | Search pill (38 dp, indigo tint, focus + keyboard on open, ✕ inside, IME Search hides keyboard); "N results for 'q'" line with All/Data/Dates/Files buckets when hits span kinds; row meta line becomes the matching field with highlight + kind badge; empty state naming the missing words; flat list while searching | shipped — pill on Home only (Upcoming/Settings never had a search field) |

### Consolidated queue as of 08-Sep-2026

| SrNo. | ID | Item | Status | Design |
|---|---|---|---|---|
| 1 | B3 | About Me to standard, last in Settings, mail pre-fill | SHIPPED 1.7 | 1.6.D1 |
| 2 | B4 | Per-file Share pill, real MIME, no silent degrade | SHIPPED 1.7 | 1.6.D1 |
| 2a | N58 | Card density Compact · Comfortable · Detailed | SHIPPED 1.7 | 1.6.D2 |
| 2b | N59 | Tap to expand | SHIPPED 1.7 partially — three values + Share in the expanded row still queued | 1.6.D2 |
| 2c | B5 | Files never in the text; two-step send | SHIPPED 1.7 | 1.6.D3 |
| 2d | B6 | Footer optional, default off | SHIPPED 1.7 — superseded by B9 (footer removed entirely) | 1.6.D3 |
| 2e | B7 | Card gap 6 dp (4 dp Compact), sections 8 dp | SHIPPED 1.8 | 1.7.D1 |
| 2f | B8 | Share in the expanded row + three data values | SHIPPED 1.8 (picker opens on the card view) | 1.7.D1 |
| 2g | B9 | "Shared from InfoDeck" removed entirely | SHIPPED 1.8 | 1.7.D1 |
| 2h | B10 | Exact file names when sharing | SHIPPED 1.8 | 1.7.D1 |
| 2i | B11 | Search reads every field | SHIPPED 1.9 | 1.8.D1 |
| 2j | B12 | Search field UX | SHIPPED 1.9 | 1.8.D1 |
| 3 | N53 leftover | Header scroll-collapse 48 -> 40 dp | approved | 1.5.D1 |
| 4 | N55 leftover | Recolour card-view section rows and Settings group tiles | approved | 1.5.D1 |
| 5 | N57 leftover | Zip packing progress, "Export everything as .zip" | DORMANT after B2 unless revived | 1.5.D2 |
| 6 | N46 | Alert modes Notify / Ring / Alarm; schema 2 -> 3 | queued, needs explicit approval | 1.4.D1 |
| 7 | N47 | Four app icons + switcher | queued, needs explicit approval | 1.4.D1 |
| 8 | N48 | Document auto-fill (DOCX + text PDF) | queued, needs explicit approval | 1.4.D1 |
| 9 | N49 | Drawer replacing the bottom bar | queued; CONFLICTS with N54's 58 dp bar - Krishna must choose | 1.4.D1 |
| 10 | N50 leftover | Settings ▸ Sharing; CSV export; share from selection mode | approved | 1.4.D2 |
| 11 | N51/N52 leftovers | Per-card share memory; QR brightness boost | approved | 1.4.D2 |
| 12 | N45 leftover | Undo snackbars; Discard dialog listing old -> new | approved | 1.3.D2 |
| 13 | N40/N42 leftovers | Search buckets; per-tab memory; swipe-to-snooze; quick card (density superseded by N58) | approved as extras | 1.3.D1 |
| 14 | older | Section icon tiles, motion, illustrated empty states, Filter-sheet tag block, "Backup with files" | approved | 1.2.D1 |
| 15 | hygiene | Dead-code sweep: HomeHero, HomeBottomBar, ControlRow, QuickChips | - | - |
| 16 | project-wide | About Me correction to Remindly, Contact Cleaner, Kalam, ChargeWatch at their next release | approved with "everything" | - |

Still blank from Krishna: "bug 2" from the 1.3 round, and item "3." from the 1.2 round.
Next build = **1.7**, versionCode **1007000**; RELEASE_DATE is set on the build day.


### v1.6 — versionCode 1006000 — 20-Aug-2026 — SHIPPED

Q1 from the 1.5.D1 round was never answered, so both readings of "bottom card deck" were built:
the tab bar back to 58 dp AND roomier card rows.

| SrNo. | ID | Item | Status |
|---|---|---|---|
| 1 | N53 | White 48 dp top bar with a 3 dp gradient hairline; blue slab gone | shipped (scroll-collapse NOT built) |
| 2 | N54 | Bottom bar back to 58 dp with labels and a hue per tab | shipped |
| 3 | N55 | Nine-hue colour system (`core/Hues.kt`), light and dark derived | shipped (card-view rows and Settings tiles NOT recoloured) |
| 4 | N56 | Category glyphs on a gradient tile leading every card row | shipped |
| 5 | N57 | File sharing in the picker: Attach vs Bundle, badges, warnings, zip bundler | shipped — and FAILED on device (see B4 below); superseded |

### APPROVED 08-Sep-2026 ("add everything to queue") — drawn in `infodeck-1.6.D1-design.html`, nothing built
(IDs B3/B4 — B1/B2 were already used by the 1.2 snooze and delete bugs)

| SrNo. | ID | Item | Three companions |
|---|---|---|---|
| A | B3 | **About Me rebuilt to the project standard (ultra-compact-B).** 1.6 breached it eight ways: no accent header strip, no KB avatar, no "Developer" caption, LinkedIn/Facebook printed as full URLs, "Suggestion / Feedback / Bugs / Error" was dead text not chips, no ✉ Send chip, mail had NO subject or body pre-fill, and the card sat INSIDE the collapsible "Diagnostics and About" group. Fix: four rows (header strip ~34 dp in accent tint · identity row with 34 dp KB avatar + name + "Developer" + LinkedIn/Facebook as 30 dp circular buttons · chips Suggestion/Feedback(default)/Bug/Error + filled ✉ Send with the address as a caption · two-line centred footer "InfoDeck · <version>" then "Released on dd-MMM-yyyy"), never collapsible, always expanded, LAST in Settings outside the accordion; accent #3B5BDB; full mail pre-fill with the nine-line context block | values read defensively, missing BuildConfig value renders "unknown"; mail and link taps guarded; `Logger.e` on failed mail intent, failed link, missing value; JUnit on the subject for all four chip types, the body block, the footer format, and that no sensitive key appears in the body |
| B | B4 | **Share reverted to text-only + per-file Share button.** Root cause in source: the send intent used `type = "*/*"` although `FileShare.commonMime()` existed and returns the real type; with an explicit package that often matched no activity, the catch block quietly fell back to a TEXT-ONLY chooser. Fix: picker returns to the 1.5 field-only sheet (file rows, Attach/Bundle, badges, size and masking warnings removed, SMS returns); every file row gains a green **Share** pill beside Open sending that ONE file with its true MIME through the system chooser, no package pinning. Any degrade to text-only must be announced. Bundler kept but unreachable | missing file reported not sent, URI only via FileProvider; `Logger.e` on missing file, failed grant, any degrade; JUnit that the intent MIME equals `FileShare.mime(name)` for every extension and that no FILE field can reach the picker |

### Drawn in `infodeck-1.5.D2-design.html` — awaiting approval, nothing built

| SrNo. | ID | Item | Three companions |
|---|---|---|---|
| Z | N57 | **Share the attached files with the card.** Per-file checkboxes in the existing "what to share" picker; one file -> `ACTION_SEND` + `EXTRA_STREAM`, several -> `ACTION_SEND_MULTIPLE`, both through the FileProvider already proven by the QR PNG path in 1.5. Optional **"Bundle as one .zip"** (card text + every ticked file) so the text can never be dropped. SMS excluded - Android cannot carry an attachment there. **Masking warning**: if any value is masked AND a file is ticked, warn that the file itself may still show it | file URI built through FileProvider only, a missing/deleted file is dropped from the send with a message rather than crashing the intent, zip written to cache and deleted after; `Logger.e` on missing file, failed URI grant, failed zip, oversize warning; JUnit on MIME resolution, single-vs-multiple selection, zip naming and the masked-plus-attached warning rule; device checklist across WhatsApp / Gmail / Drive since text-plus-document behaviour differs per app and CANNOT be verified in this container |

Design adds: per-file rows with size and date (files start UNTICKED), an Attach/Bundle switch that appears
once a file is ticked, per-app badges (green both fine / amber text may drop / red files impossible, SMS
greyed out entirely), a 25 MB size warning BEFORE an app is chosen, "Export everything as .zip", packing
progress with cancel, missing files skipped loudly, and the masking-conflict warning with three honest
ways out. Zip = a .txt of the card plus every ticked file, built in cache under a temporary name, renamed
on success, deleted when the share returns. Per-app text-with-attachment behaviour is labelled
UNVERIFIED ON DEVICE in the design, with the clipboard copy as the fallback so text is never lost.

### Round drawn in `infodeck-1.5.D1-design.html` — awaiting approval, nothing built

| SrNo. | ID | Item | Three companions |
|---|---|---|---|
| A | N53 | **Header compressed to a white 48 dp bar** — the blue gradient slab is removed entirely and replaced by a 3 dp gradient hairline; title + three coloured icon buttons; light status bar with dark icons; shrinks to 40 dp on scroll and returns on scroll-up | collapse fraction clamped, a null title falls back to the app name; `Logger.e` on an out-of-range collapse fraction; JUnit on the collapse curve and clamps, device pass in both themes |
| B | N54 | **Bottom bar back to 58 dp** — labels returned under every icon, one hue per tab (indigo Home, amber Upcoming, teal Settings), active tab keeps its tinted pill | a label never clips - it drops first; `Logger.e` on a destination with no label; JUnit that each tab maps to exactly one hue and one label, device pass at Huge text |
| C | N55 | **Nine-hue colour system** — indigo/amber/rose/green/violet/purple/teal/sky/slate, each with a fixed meaning; gradient tile + white glyph for leading icons, 12 % tint chip + saturated glyph for labels; applied to cards, filter chips, section rows, action row (Edit indigo, Copy teal, QR violet, Share green filled), settings groups; dark-theme variants at 18 % tint on #1D2431 | unknown hue index falls back to slate, dark variants derived not duplicated; `Logger.e` on unknown hue or missing tint; JUnit on hue -> tint/glyph mapping for all nine in both themes plus a 4.5:1 contrast assertion |
| D | N56 | **Category glyphs** — a default glyph per category with Krishna able to override, so every card tile carries a meaningful icon | unknown glyph key falls back to a generic card glyph; `Logger.e` on the unknown key; JUnit that every category resolves to exactly one glyph |

**Open ambiguity (Q1):** "the bottom card deck got highly compressed" has two readings — (A) the tab bar
50 -> 58 dp, (B) the card rows 62 -> 76 dp. Both are drawn; my recommendation is both, paid for by the
header. Nothing is built until Krishna picks.

If accepted these ship as **1.6**, versionCode **1006000**, no schema change.


### v1.5 — versionCode 1005000 — 20-Aug-2026 — SHIPPED

Krishna's correction applied: action row order is **Edit | Copy | QR | Share**, Share last and primary
(matching Save on the right of the action bar).

| SrNo. | ID | Item | Status |
|---|---|---|---|
| 1 | N50 | Share made prominent — action row on the card view, share icon in the top bar, direct-to-app targets (WhatsApp / Gmail / SMS / Email, installed-only via the manifest `<queries>` block), .txt export through the system picker, chooser fallback | shipped (Settings ▸ Sharing screen and CSV-for-many-cards NOT built) |
| 2 | N51 | Field picker before every share — checkboxes over name/category/data/dates/reminders, presets Last used / Everything / IDs only / Dates only, per-value masking, live preview of the exact text | shipped (per-card memory of the selection NOT built — it resets to Last used each time) |
| 3 | N52 | QR codes — per-data-row QR button, single value or combined code, one-code-per-field with a pager, encoded text printed underneath, capacity guard, save as 1024 px PNG, share the image | shipped (automatic screen-brightness boost NOT built) |

NOT built in 1.5 and still queued: Settings ▸ Sharing (favourite-app picker, default fields, footer
toggle, file-name pattern), CSV export for several cards, share from selection mode, per-card memory
of the share selection, QR brightness boost — plus everything from the 1.4.D1 round (N46 alert modes,
N47 app icons, N48 auto-fill, N49 drawer), which was never approved.

### Original spec drawn in `infodeck-1.4.D2-design.html`

| SrNo. | ID | Item | Three companions |
|---|---|---|---|
| A | N50 | **Share made prominent** — primary gradient Share button in a card-view action row (Share / QR / Copy / Edit), share icon in the top bar, Share as the filled first action in selection mode; ⋮ keeps only Move, Duplicate, Danger zone. **Favourite apps** WhatsApp / Gmail / SMS / Email opening directly (changeable in Settings, installed-only via a manifest `<queries>` block), plus **Export as .txt** and **CSV for many cards** through the system picker | missing target app falls back to the chooser, empty selection is blocked not sent; `Logger.e` on missing package, failed intent, empty share; JUnit on the text builder (order, masking, footer, empty), device pass into all four apps |
| B | N51 | **Field picker before every share** — checkbox sheet over card name, category/tags, each data variable, each date, reminders and file attachment; presets Last used / Everything / IDs only / Dates only; **remembered per card**; per-value **masking** (first four characters kept); live preview of the exact text | remembered selections validated against the card as it is now, stale fields dropped; `Logger.e` on a stale remembered field; JUnit on presets, per-card memory, stale pruning, CSV column union |
| C | N52 | **QR codes** — per data row button, single-value or multi-field code (switchable to one code per field with a pager), encoded text always printed underneath, capacity guard at ~1200 chars, full-screen brightness boost restored on exit, save as 1024 px PNG or share the image. On-device encoder, ~150 KB | over-capacity refused before drawing, failed PNG write leaves no partial file, brightness restored even on a crash path; `Logger.e` on over-capacity, encode failure, failed save; JUnit on capacity limits, multi-field builder, per-field splitting, file naming |

Folds into the same release as N46-N49 -> **1.5**, versionCode **1005000**. No new tables; one small
per-card preference blob for the remembered share selection; manifest gains a `<queries>` block and
still has **no INTERNET**.

### Round drawn in `infodeck-1.4.D1-design.html` — awaiting approval, nothing built

| SrNo. | ID | Item | Three companions |
|---|---|---|---|
| A | N46 | **Alert modes per scheduler — Notify / Ring / Alarm.** Notify = today's behaviour; Ring = heads-up + looping sound on the notification stream with a stop-after; Alarm = full-screen over the lock screen, alarm stream, `setAlarmClock`, Snooze/Stop only. Three channels, per-mode options, app-wide default (Notify), three test buttons, permission rows for full-screen intent / DND / battery. **Schema 2→3: new `alertMode` column defaulting to NOTIFY** | unknown mode falls back to Notify, missing full-screen permission degrades Alarm to Ring, sound service always self-stops on timeout; `Logger.e` on degrade, failed sound start, unknown mode; JUnit on mode→channel/stream/flag mapping, degrade rule, stop-after clamp + device checklist under DND / silent / locked |
| B | N47 | **Four app icons + in-app switcher** — A card stack, B card in a countdown ring (my pick), C folded document, D "iD" monogram; each with background, foreground and monochrome layers; switching via one `activity-alias` per icon | alias switch is one transaction, a failure leaves the current icon enabled; `Logger.e` on a failed switch; JUnit that exactly one alias is enabled per selection + device pass on three launchers |
| C | N48 | **Document auto-fill on new cards** — attach a PDF/DOCX and InfoDeck reads it on-device and PROPOSES label, data variables, date variables (with the key date ★) and reminders, each with source snippet and confidence; only high confidence pre-ticked; nothing written without a tick; new cards only. Phase 1 = DOCX (no dependency) + text PDFs (+6-10 MB); scanned PDFs offered manual entry, OCR deliberately deferred | extraction off-thread with a hard timeout, corrupt/encrypted file yields "could not read" not a crash, proposals capped at 8 data / 4 dates / 3 reminders; `Logger.e` on unreadable file, timeout, zero result, each rejected proposal; JUnit on five date formats, key-date selection, field patterns, confidence banding, caps, and a five-document fixture set |
| D | N49 | **Drawer replaces the bottom bar** — hamburger in the slim bar with the overdue badge, left-edge swipe, destinations Home / Upcoming / Categories / Tags / Settings / App icon / Error logs with counts; 50 dp returned to every screen; no drawer handle on editors | unknown destination falls back to Home; `Logger.e` on the unknown destination; JUnit on the destination list and badge counts |

If accepted these ship as **1.5**, versionCode **1005000**, schema **2→3**.


### v1.4 — versionCode 1004000 — 18-Aug-2026 — SHIPPED

| SrNo. | ID | Item | Status |
|---|---|---|---|
| 1 | N40 | Compact chrome — slim 56 dp top bar, 30 dp filter strip with counts (zeros hidden), View button, count line dropped, wordless "+" FAB | shipped (collapsing-on-scroll NOT done) |
| 2 | N41 | Slim 50 dp persistent bottom bar — Home / Upcoming / Settings, icon-only with the word on the selected tab, overdue badge on Home | shipped (per-tab scroll memory NOT done) |
| 3 | N42 | Search as an icon that expands in the bar — Home, Upcoming, Settings | shipped (result buckets Cards/Tags/Files NOT done; Home search still filters cards only) |
| 4 | N43 | Settings grouped and collapsible, one open at a time, Categories + Tags moved into **Organise** | shipped |
| 5 | N45 | Two-button action bar — Cancel while clean, Discard when dirty, Undo removed from the bar | shipped (undo snackbars for delete/move/row-removal and the change-listing Discard dialog NOT done) |

STILL QUEUED after 1.4: undo snackbars (delete, bulk delete, move, row removed, reset), Discard dialog
listing old -> new per change, collapsing top bar, search result buckets, density Compact/Cozy/Roomy,
swipe-right to snooze, long-press "+" quick card, per-tab memory, section icon tiles, motion polish,
illustrated empty states, Undo after card delete, Filter-sheet tag block, "Backup with files".

### Original spec drawn in `infodeck-1.3.D2-design.html`

| SrNo. | ID | Item | Three companions |
|---|---|---|---|
| Z | N45 | **Drop the Undo button from the action bar** — drawn in `infodeck-1.3.D2-design.html` (my recommendation after his challenge): bar becomes **Cancel / Discard + Save**, two buttons, with the left button labelled "Cancel" while nothing is pending and "Discard" once it is. Undo survives only as a **snackbar** after committed destructive or bulk actions, plus an inline "row removed - UNDO" inside editors. Six snackbar places specced (delete, bulk delete, move, row removed, reset, save); Erase everything deliberately offers none. Discard dialog now LISTS the actual changes (old -> new, five then "and N more"). Edited fields carry a blue rail. `UndoStack` is kept and repurposed as the snapshot store behind those snackbars, so its 8 tests stay green. Widths: 2 x 161 dp instead of 3 x 104 dp, which is what stops "Discard" truncating at Large text | snapshot restore stays transactional; `Logger.e` on a failed restore; JUnit retargeted from the bar to the snackbar paths, plus a test that the left button's label follows the dirty state |

### Round drawn in `infodeck-1.3.D1-design.html` — awaiting approval, nothing built

| SrNo. | ID | Item | Three companions |
|---|---|---|---|
| A | N40 | **Compact chrome** — hero 168 dp → slim 56 dp top bar; three stat blocks → one 30 dp filter strip with counts (zero-count chips hidden); Group+Sort+Filter → one "View" button; count line dropped; collapsing top bar on scroll; wordless round "+" FAB with 62 dp list padding. Net **≈138 dp** back to the list | chip list built from counts, negative/zero counts drop the chip; `Logger.e` on a negative count; JUnit on the visible-chip rule and the collapse threshold |
| B | N41 | **Slim persistent bottom bar** — 50 dp, icons only with the word on the selected tab, destinations Home / Upcoming / Settings, visible on every top-level and Settings sub-screen, hidden only on editors, card view, selection mode and sheets; per-tab scroll and filter memory | destination resolution total, unknown route falls back to Home; `Logger.e` on unknown route; JUnit on the visible/hidden rule per route, device pass on rotation and back |
| C | N42 | **Search everywhere as an icon** — expands in place inside the 56 dp bar on Home, Upcoming, Settings, Categories and Tags; Home results bucketed Cards / Tags / Files with file-name matching and highlighted matches | blank/whitespace query returns the full list, queries trimmed at 100 chars; `Logger.e` on a search failure; JUnit on bucket counts, file-name match and the trim rule |
| D | N43 | **Settings grouped and collapsible** — eight groups (Organise / Look and feel / Reminders / Typing / Data and backup / Security / Diagnostics and About / Danger zone), all collapsed with summary lines, one open at a time, last-opened remembered. **Categories and Tags move into Organise**, plus default category for "+" and Home grouping | unknown stored group id falls back to all-collapsed; `Logger.e` on the unknown id; JUnit that every settings row belongs to exactly one group and none was lost in the move |
| E | N44 | **Optional extras (§C7)** — density Compact/Cozy/Roomy, settings search, swipe-right to snooze (never swipe-to-delete), long-press "+" quick card, overdue badge on the Home tab icon | per-item, specced when Krishna picks which to keep |

If accepted these ship as **1.4**, versionCode **1004000**, no schema change.


### v1.3 — versionCode 1003000 — 18-Aug-2026 — SHIPPED

| SrNo. | ID | Item | Status |
|---|---|---|---|
| 1 | N36 | Visual overhaul — gradient hero, stat pills, search pill, countdown-ring cards, overdue wash, bottom navigation, new palette | shipped (partial: section icon tiles and motion polish not done — see below) |
| 2 | N37 | Save · Undo · Discard bar on every editing surface, with confirmed Discard and Saved+Undo snackbar | shipped |
| 3 | N38 | No reset without confirmation — text reset, filter clear-all, new "Put settings back to normal", "Erase everything" with typed ERASE | shipped |
| 4 | N39 | Friendly text customisation — named sizes, two A buttons, size cards, live draft preview, Easy-read mode, Smaller/Normal/Bigger groups | shipped |

NOT done in 1.3 and still queued: section header icon tiles, press/expand motion, the illustrated
empty states, Card style setting (Ring/Bar/Plain), pinch-to-zoom on the new hero layout, Undo after
card delete, the Filter sheet tag block, and "Backup with files".

### Original spec drawn in `infodeck-1.2.D1-design.html`

| SrNo. | ID | Item | Three companions |
|---|---|---|---|
| A | N36 | **Visual overhaul** — gradient hero + stat pills, search pill, countdown-ring cards, overdue wash, section icon tiles, bottom navigation (Home/Upcoming/Tags/Settings), soft-card form fields, illustrated empty states, press/expand motion | every drawing helper total (bad colour index or null date falls back to neutral); `Logger.e` on out-of-range ring fraction or unknown card style; JUnit on ring fraction (overdue/today/far/no date) + colour bands, device pass light and dark |
| B | N37 | **Save · Undo · Discard on every editing surface** — equal width 50 dp, Save gradient-filled, Undo greys at empty stack, Discard confirms when dirty, back gesture = Discard, "Saved" snackbar with 5 s Undo. Supersedes the shared Cancel+Save+Delete chrome (project-wide question Q11) | undo stack bounded at 50, never pops empty; Save validates before writing; Discard restores the open-time snapshot; `Logger.e` on failed save/restore/underflow; JUnit on stack push/undo/bound/empty, dirty count, snapshot==discard |
| C | N38 | **No reset without confirmation** — text reset, offsets reset, clear-all-filters, settings defaults all behind a naming dialog with Undo; "Reset all data" renamed **Erase everything** with typed ERASE, separated from the harmless resets | each reset is one transaction, a failure leaves settings untouched; `Logger.e` on reset failure naming the target; JUnit that each reset returns the documented defaults and touches no card data |
| D | N39 | **Friendly text customisation** — named sizes Tiny..Huge with % as subtitle, two large A buttons, five size cards, live Home preview with pinch, Easy-read mode, "What to enlarge" as 4 groups x Smaller/Normal/Bigger. Keeps the 1.2 engine (70-200 %, damping, 8-46 sp clamps) | name<->scale mapping total both ways, unknown stored name falls back to Standard; `Logger.e` on unmapped name/step; JUnit on all seven name<->percent steps, three-way choices -> offsets, Easy-read composing to the documented triple |

If accepted these ship together as **1.3**, versionCode **1003000**, no schema change.


| SrNo. | ID | Type | Item | Companions |
|---|---|---|---|---|
| 1 | B1 | Bug | Snooze = "Tomorrow" (−1) is passed straight into `now + minutes×60 000`, giving a snooze in the past | guard + clamp; `Logger.e` on out-of-range snooze; JUnit on the snooze-target calculation |
| 2 | N30 | Add | ~~Text size, 5 fixed stops~~ **REJECTED by Krishna 18-Aug-2026 — superseded by N30b** | — |
| 2b | N30b | Add | **Dynamic text size** — continuous 70-200% in 1% steps, live apply, slider + steppers + typed value + presets + pinch-to-zoom on Home, layout metrics scale with text (damped 0.6, clamp 0.80-1.60), six per-role offsets (-30..+30%), Follow-system toggle showing effective %, row spacing Compact/Comfortable/Spacious, text weight (design 1.1.D2 S1-S4) | scale clamped 0.70-2.00 and offsets clamped, corrupt value falls back to 1.00, final sp clamped 8-46, tap targets floor 44 dp; `Logger.e` on corrupt scale/offset; JUnit scale->sp for six roles at 70/100/137/200% + damping + clamps, device check every screen at 70% and 200% |
| 2c | N34 | Add | **File variables (PDF/DOCX)** — copied into app-private storage, 25 MB x 10 per card, open/share via FileProvider, rename/replace, removal behind the danger layer, "Backup with files" zip, storage line in Settings (design 1.1.D2 S5/S6/S9) | type/size/count gate before copy, temp-then-rename so a failed copy leaves nothing, missing file shows "File missing" not a crash; `Logger.e` on rejected type, oversize, failed copy, missing file, no viewer app; JUnit on the gate + name sanitiser + zip backup byte-identical round trip, device check open/share |
| 2d | N35 | Add | **Tags** — many-to-many, editor row + picker sheet with inline create, Group by Tag (multi-tag cards appear under each, Untagged last), Filter block with Any/All, tag quick chips, Manage tags screen with counts/rename/merge/delete (design 1.1.D2 S5/S7/S8/S9) | duplicate name rejected case-insensitively before insert, merge/delete in one transaction, cascade prevents orphan links; `Logger.e` on duplicate, failed merge, failed delete; JUnit name normalisation, merge maths, Any/All filter, multi-tag grouping counts, **migration v1->v2 asserting zero data loss** |
| 3 | N31 | Add | **Auto-capitalise words** on every text field except search, with an All-fields / Names-only scope (S3, S6) | scope decode falls back to All fields; `Logger.e` on bad scope; JUnit on the field to capitalisation map |
| 4 | N32 | Modify | **Group by "None" shows the card deck** — no "All cards" wrapper, no chevron; search renders identically (S1, S2) | empty list still shows the empty state; existing Home guard; JUnit None has no section wrapper + negative control that Category still groups |
| 5 | N33 | Modify | **Delete behind two layers everywhere** — Danger zone entry, danger sheet, then confirm; editor bar drops to Cancel+Save; Undo snackbar (S6-S9) | Undo restore inside one transaction; `Logger.e` on failed delete/restore; JUnit snapshot-restore round trip + device walk of all five delete doors |
| 6 | N26 | Add | Drag-to-reorder cards inside a category (true Manual order) | persistence guard; log reorder failures; JUnit on reorder maths |
| 7 | N27 | Add | Home-screen widget: next N expiries | — |
| 8 | N28 | Add | Per-card colour/icon | — |
| 9 | N29 | Modify | Attach a photo/scan to a card | — |

Schema note: N34 + N35 take the database from **v1 to v2** by an additive migration (three new tables:
`tags`, `card_tags`, `files`). No existing table is touched. Backup format goes to v2 and still reads v1.

N30b-N35 are **specced and drawn** in `infodeck-1.1.D1-design.html` and `infodeck-1.1.D2-design.html`; they stay unbuilt until the design is
approved and an explicit build trigger is given. If built together with B1 the release is a mixed one:
y bumps and z is dropped -> **1.2**, versionCode **1002000**.

