#!/usr/bin/env bash
# Usage: package.sh <SoftwareName> <Version> <staging dir> [android.apk] [windows-x64.exe] [portable.exe] [macOS.dmg]
# Lays out the one artifact defined in docs/standards/workflows.md:
#   <SoftwareName>_<Version>/  portable/ windows-x64/ macOS/ Android/
# A platform with no build is skipped - its folder is simply absent.
set -euo pipefail
name="$1"; ver="$2"; out="$3"; apk="${4:-}"; exe="${5:-}"; portable="${6:-}"; dmg="${7:-}"
base="${name}_${ver}"
rm -rf "$out"; mkdir -p "$out"
[ -n "$portable" ] && [ -f "$portable" ] && mkdir -p "$out/portable"    && cp "$portable" "$out/portable/$base.exe"
[ -n "$exe" ]      && [ -f "$exe" ]      && mkdir -p "$out/windows-x64" && cp "$exe"      "$out/windows-x64/$base.exe"
[ -n "$dmg" ]      && [ -f "$dmg" ]      && mkdir -p "$out/macOS"       && cp "$dmg"      "$out/macOS/$base.dmg"
[ -n "$apk" ]      && [ -f "$apk" ]      && mkdir -p "$out/Android"     && cp "$apk"      "$out/Android/$base.apk"
if [ -z "$(ls -A "$out")" ]; then echo "::error::nothing to package"; exit 1; fi
( cd "$out" && find . -type f | sort )
