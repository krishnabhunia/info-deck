#!/usr/bin/env bash
# Fails if anything that must never be public is tracked in git:
# signing keys, keystores, private keys, or a password written into a build file.
# Runs in CI on every push and PR, and can be run locally before committing.
set -u
cd "$(git rev-parse --show-toplevel)"
fail=0

bad_files=$(git ls-files | grep -Ei '\.(keystore|jks|p12|pfx|pem|key)$|(^|/)local\.properties$|(^|/)keystore\.properties$' || true)
if [ -n "$bad_files" ]; then
  echo "::error::Key or keystore files are tracked - remove them and use GitHub secrets:"; echo "$bad_files"; fail=1
fi

pw=$(git grep -nEi '(store|key)Password[[:space:]]*=[[:space:]]*"[^"]+"|System\.getenv\("[^"]*(PASS|SECRET|TOKEN)[^"]*"\)[[:space:]]*\?:[[:space:]]*"[^"]+"' -- '*.kts' '*.gradle' '*.properties' || true)
if [ -n "$pw" ]; then
  echo "::error::A password literal is written into a build file:"; echo "$pw"; fail=1
fi

pk=$(git grep -nE -- '-----BEGIN ([A-Z]+ )?PRIVATE KEY-----' || true)
if [ -n "$pk" ]; then
  echo "::error::A private key is committed:"; echo "$pk"; fail=1
fi

if [ "$fail" = 0 ]; then echo "secret scan: clean ($(git ls-files | wc -l) tracked files checked)"; fi
exit $fail
