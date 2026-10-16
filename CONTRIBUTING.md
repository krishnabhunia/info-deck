# Contributing

## Pull request names

| Opened by | Title must start with |
|---|---|
| Claude (Anthropic) | `cld-` — e.g. `cld-search-buckets` |
| ChatGPT (OpenAI) | `cgpt-` — e.g. `cgpt-fix-share-names` |
| Krishna | any |

## Every PR needs exactly one release label

| Label | Meaning | Version effect |
|---|---|---|
| `release:feature` | any new, added or changed feature (including mixed feature + fix) | y + 1, z = 0 — 1.9.0 → 1.10.0 |
| `release:fix` | bug fixes only | z + 1 — 1.10.0 → 1.10.1 |
| `release:major` | major version — **applied only by Krishna** | x + 1, y and z reset → x.0.0 — 1.15.0 → 2.0.0 |
| `release:none` | docs, CI or tooling only | unchanged, nothing published |

All new versions use three-part `x.y.z` notation. The workflow fails the PR until one label is present, then writes the new number into
`VERSION` on the PR branch itself using the dedicated GitHub App described in [docs/ci-app-token.md](docs/ci-app-token.md). If the App has not been configured and a bump is needed, the workflow fails; it does not use the default `GITHUB_TOKEN` for this push. Nobody edits `VERSION` by hand.

## What a PR produces

* every push to the PR → a **beta** pre-release `v<next>-beta.<run>` with `InfoDeck_<next>-beta.<run>.zip`
* merging into `main` → the **release** `v<next>` with `InfoDeck_<next>.zip`

## Never commit

Keystores (`*.keystore`, `*.jks`), key files, `local.properties`, passwords or tokens.
`common/scripts/secret_scan.sh` runs on every push and fails the build if any of these appear.

## Working rules carried from the project

* Design first — every change gets an HTML phone-frame design in `docs/design/` before code.
* Every change carries its three companions: exception handling, `Logger.e` logging, tests.
* Release builds only; verify with `aapt2 dump badging` and `apksigner verify`.
