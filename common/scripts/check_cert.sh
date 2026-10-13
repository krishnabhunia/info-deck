#!/usr/bin/env bash
# Usage: check_cert.sh <apk> <expected SHA-1 with or without colons>
# Fails unless the APK is signed with the expected certificate, so a CI-built APK
# always installs over the one already on the phone.
set -euo pipefail
apk="$1"; want=$(echo "$2" | tr -d ':' | tr 'A-F' 'a-f')
signer=$(ls "$ANDROID_HOME"/build-tools/*/apksigner | sort -V | tail -1)
"$signer" verify "$apk"
got=$("$signer" verify --print-certs "$apk" | awk -F': ' '/certificate SHA-1 digest/{print $2; exit}' | tr -d ':' | tr 'A-F' 'a-f')
if [ "$got" != "$want" ]; then
  echo "::error::APK signed with SHA-1 $got, expected $want - it would NOT install over the existing app"; exit 1
fi
echo "signing certificate: OK ($got)"
