#!/usr/bin/env bash
# Usage: check_cert.sh <apk> <expected SHA-1 with or without colons>
# Fails unless the APK is signed with the expected certificate, so a CI-built APK
# always installs over the one already on the phone.
set -euo pipefail
apk="$1"; want=$(echo "$2" | tr -d ':' | tr 'A-F' 'a-f')
signer=$(ls "$ANDROID_HOME"/build-tools/*/apksigner | sort -V | tail -1)
"$signer" verify "$apk"
# apksigner labels signers by number or SDK range, optionally with a colon.
# Read the final field, never the label, and accept repeated reports of the
# same certificate across SDK ranges. Every distinct signer must match.
# Capture the full successful output before parsing so pipefail also catches
# verification failures and the parser does not terminate apksigner early.
certs=$("$signer" verify --print-certs "$apk")
got=$(printf '%s\n' "$certs" | awk -F': ' '/^Signer .*certificate SHA-1 digest: /{print $NF}' | tr -d ':\r' | tr 'A-F' 'a-f' | sort -u)
if [[ ! "$got" =~ ^[0-9a-f]{40}$ ]] || [ "$got" != "$want" ]; then
  # Public certificate fingerprints help diagnose format changes without keys.
  printf '%s\n' "$certs" | sed -n '/certificate SHA-1 digest: /p'
  echo "::error::APK signed with SHA-1 $got, expected $want - it would NOT install over the existing app"; exit 1
fi
echo "signing certificate: OK ($got)"
