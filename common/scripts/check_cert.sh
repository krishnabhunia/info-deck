#!/usr/bin/env bash
# Usage: check_cert.sh <apk> <expected SHA-1 with or without colons>
# Fails unless the APK is signed with the expected certificate, so a CI-built APK
# always installs over the one already on the phone.
set -euo pipefail
apk="$1"; want=$(echo "$2" | tr -d ':' | tr 'A-F' 'a-f')
signer=$(ls "$ANDROID_HOME"/build-tools/*/apksigner | sort -V | tail -1)
"$signer" verify "$apk"
# Newer apksigner versions print "Signer #1: certificate ...", while older
# versions omit that first colon. Read the final field, never the label.
# Capture the full successful output before parsing so pipefail also catches
# verification failures and the parser does not terminate apksigner early.
certs=$("$signer" verify --print-certs "$apk")
got=$(printf '%s\n' "$certs" | awk -F': ' '/^Signer #[0-9]+:? certificate SHA-1 digest: /{print $NF}' | tr -d ':\r' | tr 'A-F' 'a-f')
if [[ ! "$got" =~ ^[0-9a-f]{40}$ ]] || [ "$got" != "$want" ]; then
  echo "::error::APK signed with SHA-1 $got, expected $want - it would NOT install over the existing app"; exit 1
fi
echo "signing certificate: OK ($got)"
