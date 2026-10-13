# macOS/

Reserved for a macOS build. InfoDeck is Android-only today, so this folder holds no code and
the **macOS** job in `.github/workflows/build.yml` is skipped.

When code lands here, the workflow detects it and the job must produce
`InfoDeck_<version>.dmg` for `macOS/` in the release zip.
