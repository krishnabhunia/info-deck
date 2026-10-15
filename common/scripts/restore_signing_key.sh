#!/usr/bin/env bash
# Restore CI signing material without printing any secret. Never build a download unsigned.
set -euo pipefail
missing=()
for name in KEYSTORE_B64 INFODECK_STOREPASS INFODECK_KEYPASS; do
  [ -n "${!name:-}" ] || missing+=("$name")
done
if [ "${#missing[@]}" -ne 0 ]; then
  echo "::error::Android signing is required. Configure INFODECK_KEYSTORE_B64, INFODECK_STOREPASS and INFODECK_KEYPASS with the original key. See docs/release-process.md. No APK will be uploaded."
  exit 1
fi
: "${RUNNER_TEMP:?RUNNER_TEMP is required}"
: "${GITHUB_ENV:?GITHUB_ENV is required}"
key="$RUNNER_TEMP/infodeck.keystore"
umask 077
if ! printf '%s' "$KEYSTORE_B64" | base64 --decode > "$key" || [ ! -s "$key" ]; then
  rm -f -- "$key"
  echo "::error::INFODECK_KEYSTORE_B64 must contain a nonempty base64-encoded original keystore. No APK will be uploaded."
  exit 1
fi
printf 'INFODECK_KEYSTORE=%s\n' "$key" >> "$GITHUB_ENV"
echo "Original signing keystore restored; APK verification is still required."
