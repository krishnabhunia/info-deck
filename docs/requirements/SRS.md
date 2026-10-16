# InfoDeck — Software Requirements Specification

| | |
|---|---|
| Product | InfoDeck — offline card and document manager for Android |
| Covers | Behaviour shipped in releases 1.1 – 1.9 |
| Owner | Krishna Dipayan Bhunia |
| Status | Living document — updated with every release PR |

Requirement IDs are stable: `FR-x.n` functional, `NFR-n` non-functional. The `Origin` column
names the backlog item that introduced each requirement (see [BACKLOG.md](../BACKLOG.md)).

## 1 · Purpose and scope

InfoDeck keeps the facts people otherwise hunt for — policy and registration numbers, expiry dates,
warranty papers, ID documents — as **cards**, each with named values, dates with reminders, tags and
attached files. Everything stays on the phone: no account, no cloud, no network permission.

Out of scope: sync, multi-user, web or desktop clients, iOS (paused project-wide).

## 2 · Definitions

| Term | Meaning |
|---|---|
| Card | One thing being tracked — a label plus data variables, date variables, schedulers, tags, files |
| Data variable | A named text value on a card (`Policy No` = `HD-2291-KX-2026`) |
| Date variable | A named date on a card; one may be starred ★ as the **key date** |
| Scheduler | A reminder rule tied to a date variable (e.g. 30 days before) |
| Category | The single grouping level; built-in *Uncategorised* cannot be removed |
| Tag | A many-to-many label across cards |
| Density | How much a Home row shows: Compact · Comfortable · Detailed |

## 3 · Functional requirements

### 3.1 Cards and categories

| ID | Requirement | Origin |
|---|---|---|
| FR-1.1 | A card has a label (≤ 60 chars), any number of data variables (name ≤ 40, value ≤ 500) and date variables | N1 |
| FR-1.2 | Every card belongs to exactly one category; *Uncategorised* is built in and never deletable or renamable | N2 |
| FR-1.3 | One date per card may be the ★ key date; the first date is starred automatically; key date drives expiry grouping, sorting and filtering | N3 |
| FR-1.4 | Categories can be added, renamed, coloured, reordered and deleted; deleting moves its cards or deletes them after typed `DELETE` | N16 |
| FR-1.5 | Cards can be moved, duplicated, have reminders switched off, or deleted — singly or in selection mode | N15 |
| FR-1.6 | Tags: pick, create, merge, rename, delete; Group by Tag; Any/All tag filter | N35 |
| FR-1.7 | Files (PDF, DOCX and others) are copied into app storage and can be opened, renamed, removed and shared | N34 |

### 3.2 Reminders

| ID | Requirement | Origin |
|---|---|---|
| FR-2.1 | Scheduler rules: same day, 1–450 days, 1–60 weeks, 1–60 months, 1–20 years, before or after the date, with a ring time | N4 |
| FR-2.2 | Out-of-range amounts clamp; month-end and 29-Feb clamp through `java.time` | N4 |
| FR-2.3 | One exact alarm for the earliest due ring, re-planned after each fire; falls back to inexact when exact alarms are denied | N5 |
| FR-2.4 | Missed rings fire on the next pass and never re-fire the same moment | N6 |
| FR-2.5 | Notifications carry Done and Snooze; tapping opens the card; Snooze "Tomorrow" never lands in the past | N7, B1 |
| FR-2.6 | Boot, app update, time and timezone changes re-plan alarms; a daily 00:05 sweep re-plans as a safety net | N8, N9 |
| FR-2.7 | Upcoming lists due rings; History keeps the last 500 ring events | N19 |

### 3.3 Home list

| ID | Requirement | Origin |
|---|---|---|
| FR-3.1 | Home is an accordion of sections, one open at a time (switchable) | N10 |
| FR-3.2 | Group by Category, Expiry month, Expiry window, Next reminder, Tag or None; section order Manual / A–Z / Nearest date | N11, N32 |
| FR-3.3 | Sort by Name, Expiry, Next reminder, Recently updated, Recently added or Manual, both directions; missing values always last | N12 |
| FR-3.4 | Filters by category, expiry window, reminder state, content and tags, with a live "Show N cards" count | N13, N35 |
| FR-3.5 | Density Compact (44 dp), Comfortable (60 dp, default), Detailed (76 dp); one card gap per density (4 / 6 / 6 dp), 8 dp between sections | N58, B7 |
| FR-3.6 | Tap to expand: a row opens in place with up to three data values and Open · Edit · Share; one open at a time | N59, B8 |
| FR-3.7 | Each row shows a category glyph tile, the key date and a countdown ring; overdue rows are washed rose | N36, N56 |

### 3.4 Search

| ID | Requirement | Origin |
|---|---|---|
| FR-4.1 | Search reads every field: label, category, every data name and value, every date name and its displayed and ISO text, file names, tags and reminder rules | B11 |
| FR-4.2 | Case-insensitive, accent-folded, partial; every word must match somewhere on the card (AND) | B11 |
| FR-4.3 | Values are also matched with separators stripped (`wb24ab` finds `WB 24 AB 1234`) | B11 |
| FR-4.4 | Results ranked label › data › date › file › reminder; debounced 150 ms; query capped at 100 chars | B11 |
| FR-4.5 | The field is a focused pill with a clear button; a count line "N results for 'q'" with Data / Dates / Files buckets when hits span kinds | B12 |
| FR-4.6 | Each result row shows the matching field with the hit highlighted and a DATA / DATE / FILE / TAG / REMINDER badge | B12 |
| FR-4.7 | No results names the words that matched nothing | B12 |

### 3.5 Sharing

| ID | Requirement | Origin |
|---|---|---|
| FR-5.1 | Card view action row Edit · Copy · QR · Share; share icon in the top bar | N50 |
| FR-5.2 | A field picker precedes every share: per-field ticks, presets, per-value masking, live preview of the exact text | N51 |
| FR-5.3 | Direct targets WhatsApp, Gmail, SMS, Email (installed only) plus the system chooser and a .txt export | N50 |
| FR-5.4 | QR per value or combined, with capacity guard, PNG save and image share | N52 |
| FR-5.5 | File names never appear in shared text | B5 |
| FR-5.6 | Chat apps get the text first, then a "N files ready · SEND FILES" prompt sends the files with their real MIME types; mail gets one send with attachments; SMS never carries files | B5 |
| FR-5.7 | Shared files carry their exact display names, never the internal storage name | B10 |
| FR-5.8 | Every file row has its own Share button | B4 |
| FR-5.9 | No signature or footer is appended to shared text | B9 |
| FR-5.10 | Any fallback from files to text-only is announced, never silent | B4 |

### 3.6 Settings, safety and data

| ID | Requirement | Origin |
|---|---|---|
| FR-6.1 | Settings are named accordion groups, all collapsed, one open at a time | N43 |
| FR-6.2 | About Me sits last, outside the accordion, never collapsible: header strip, identity with LinkedIn/Facebook buttons, Suggestion/Feedback/Bug/Error chips with Send, two-line version and release-date footer | B3 |
| FR-6.3 | The feedback mail opens pre-filled: subject `[InfoDeck <version>] <type>`, body with app, version, type, date-time, device, Android version, locale — no personal data | B3 |
| FR-6.4 | Text size 70–200 % with per-role offsets, pinch-to-zoom, Easy-read mode | N30b, N39 |
| FR-6.5 | Destructive actions never sit on a first surface; Reset needs typed `RESET`, Erase everything needs typed `ERASE` | N33, N38 |
| FR-6.6 | Editing surfaces use Cancel (clean) / Discard (dirty) + Save | N45 |
| FR-6.7 | JSON backup and restore through the system file picker, Replace all or Merge; bad JSON never crashes | N21 |
| FR-6.8 | Optional biometric / device-credential app lock, off by default; never locks the owner out | N22 |
| FR-6.9 | Error Logs screen lists every logged failure, with share and clear | N23 |

### 3.7 Themes (2.0)

| ID | Requirement | Origin |
|---|---|---|
| FR-7.1 | Three themes: Aurora (default, light), Neon Night (dark), Sunrise Wallet (custom); System follows the phone — Aurora by day, Neon at night | N64 |
| FR-7.2 | Stored values SYSTEM / LIGHT / DARK keep their 1.x meaning; SUNRISE is new; unknown values follow the phone and are logged | N64 |
| FR-7.3 | A hue keeps its meaning in every theme: amber = time, rose = trouble, green = share/OK, purple = file, violet = tag/QR, teal = settings, sky = data | N64 |
| FR-7.4 | Every screen header is a band in its role colour and the status bar continues it | N64 |
| FR-7.5 | Bottom bar Home · Upcoming · Tags · Settings; Sunrise carries New card in the centre of the bar and shows no floating button | N64 |

## 4 · Non-functional requirements

| ID | Requirement |
|---|---|
| NFR-1 | **No network.** The app declares no INTERNET permission (verified on every build) |
| NFR-2 | Every code path that can fail is guarded and logs through `Logger.e`; degrade paths are visible to the user |
| NFR-3 | Every pure rule has JUnit coverage — 278 unit tests as of 1.9 |
| NFR-4 | Release builds only, signed with the project key; `aapt2 dump badging` and `apksigner verify` pass |
| NFR-5 | Every bottom sheet reserves `max(25 % of screen height, nav bar + 24 dp)` of clear space at the bottom |
| NFR-6 | Search stays instant at hundreds of cards — one prepared index per card, one `contains` per word |
| NFR-7 | Database migrations are additive; schema 2 since 1.2 |
| NFR-8 | Minimum Android 8.0 (API 26); target API 34 |

## 5 · Proposed — not built

| ID | Requirement | Spec |
|---|---|---|
| FR-9.x | In-app update check from GitHub Releases, daily, optional beta channel | [update-checker.md](update-checker.md) — design-first, awaits approval |

Everything else still queued is listed in [BACKLOG.md](../BACKLOG.md).
