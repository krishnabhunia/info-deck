#!/usr/bin/env bash
# Usage: verify_apk.sh <apk> <display version> <stable x.y.z>
# Mandatory at both the Android upload and final ZIP boundaries.
set -euo pipefail
apk="$1"; display="$2"; version="$3"
: "${ANDROID_HOME:?ANDROID_HOME is required}"
: "${EXPECTED_CERT_SHA1:?EXPECTED_CERT_SHA1 is required}"
scripts=$(cd "$(dirname "$0")" && pwd)
if [ ! -s "$apk" ]; then
  echo "::error::Signed Android APK is missing or empty. No download will be packaged."
  exit 1
fi
# Check signatures even when a filename claims this is a signed release.
bash "$scripts/check_cert.sh" "$apk" "$EXPECTED_CERT_SHA1"
aapt2=$(find "$ANDROID_HOME/build-tools" -mindepth 2 -maxdepth 2 -name aapt2 -type f | sort -V | tail -1)
badging=$("$aapt2" dump badging "$apk")
code=$(python3 "$scripts/version.py" code "$version")
header=$(printf '%s\n' "$badging" | head -1)
for expected in "name='com.krishna.infodeck'" "versionCode='$code'" "versionName='$display'"; do
  if [[ " $header " != *" $expected "* ]]; then
    echo "::error::Android package identity/version mismatch: expected $expected"
    exit 1
  fi
done
for permission in android.permission.INTERNET android.permission.REQUEST_INSTALL_PACKAGES; do
  if ! grep -Fq "uses-permission: name='$permission'" <<< "$badging"; then
    echo "::error::Missing Android update permission: $permission"
    exit 1
  fi
done
echo "$header"
echo "Android package, version, permissions and original signing certificate: verified"
