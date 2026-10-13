# windows/

Reserved for a Windows build. InfoDeck is Android-only today, so this folder holds no code and
the **Windows** job in `.github/workflows/build.yml` is skipped.

When code lands here, the workflow detects it and the job must produce
`InfoDeck_<version>.exe` for `windows-x64/` and `portable/` in the release zip.
