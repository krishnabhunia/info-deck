#!/usr/bin/env python3
"""Exercise missing credentials and APK rejection without using production keys."""
import base64
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SCRIPTS = ROOT / "common/scripts"
CERT = "8250bd7b804121d77c9152fa3c6fc76b19dd149a"


class SigningTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="infodeck-signing-")
        self.addCleanup(self.temp.cleanup)
        self.folder = Path(self.temp.name)
        self.env = dict(os.environ)
        self.env.update(
            RUNNER_TEMP=self.folder.as_posix(),
            GITHUB_ENV=(self.folder / "env").as_posix(),
            KEYSTORE_B64=base64.b64encode(b"test fixture only").decode(),
            INFODECK_STOREPASS="fixture-store-password",
            INFODECK_KEYPASS="fixture-key-password",
            EXPECTED_CERT_SHA1=CERT,
            ANDROID_HOME=(self.folder / "sdk").as_posix(),
            TEST_CERT=CERT,
            TEST_SIGNER_PREFIX="V2 Signer",
            TEST_SIGNER_SEPARATOR=":",
            TEST_SIGNATURE="valid",
            TEST_PACKAGE="com.krishna.infodeck",
            TEST_CODE="1014000",
            TEST_NAME="1.14.0-beta.1",
            TEST_PERMISSION="android.permission.REQUEST_INSTALL_PACKAGES",
        )
        tools = self.folder / "sdk/build-tools/34.0.0"
        tools.mkdir(parents=True)
        fixtures = {
            "apksigner": '''#!/usr/bin/env bash
if [ "$TEST_SIGNATURE" != valid ]; then echo 'DOES NOT VERIFY' >&2; exit 1; fi
if [ "${TEST_NO_CERT:-false}" != true ]; then
  echo "${TEST_SIGNER_PREFIX}${TEST_SIGNER_SEPARATOR} certificate SHA-1 digest: $TEST_CERT"
  if [ -n "${TEST_EXTRA_CERT:-}" ]; then
    echo "Signer (minSdkVersion=33, maxSdkVersion=2147483647) certificate SHA-1 digest: $TEST_EXTRA_CERT"
  fi
fi
''',
            "aapt2": '''#!/usr/bin/env bash
echo "package: name='$TEST_PACKAGE' versionCode='$TEST_CODE' versionName='$TEST_NAME'"
echo "uses-permission: name='android.permission.INTERNET'"
echo "uses-permission: name='$TEST_PERMISSION'"
''',
        }
        for name, content in fixtures.items():
            path = tools / name
            path.write_text(content, encoding="utf-8", newline="\n")
            path.chmod(0o755)
        self.apk = self.folder / "renamed.apk"
        self.apk.write_bytes(b"APK tool fixture")

    def run_script(self, name, *args):
        return subprocess.run(
            [shutil.which("bash"), (SCRIPTS / name).as_posix(), *map(str, args)],
            env=self.env, cwd=ROOT, capture_output=True, text=True,
        )

    def verify(self):
        return self.run_script("verify_apk.sh", self.apk.as_posix(), "1.14.0-beta.1", "1.14.0")

    def test_each_missing_credential_fails_without_exporting_a_key(self):
        for name in ("KEYSTORE_B64", "INFODECK_STOREPASS", "INFODECK_KEYPASS"):
            with self.subTest(name=name):
                previous = self.env.pop(name)
                result = self.run_script("restore_signing_key.sh")
                self.env[name] = previous
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("::error::", result.stdout)
                self.assertFalse((self.folder / "env").exists())
                self.assertFalse((self.folder / "infodeck.keystore").exists())

    def test_invalid_base64_fails_and_removes_partial_key(self):
        self.env["KEYSTORE_B64"] = "not valid base64!"
        result = self.run_script("restore_signing_key.sh")
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse((self.folder / "infodeck.keystore").exists())
        self.assertFalse((self.folder / "env").exists())

    def test_restore_exports_path_without_logging_secrets(self):
        result = self.run_script("restore_signing_key.sh")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual((self.folder / "infodeck.keystore").read_bytes(), b"test fixture only")
        self.assertIn("INFODECK_KEYSTORE=", (self.folder / "env").read_text())
        for name in ("KEYSTORE_B64", "INFODECK_STOREPASS", "INFODECK_KEYPASS"):
            self.assertNotIn(self.env[name], result.stdout + result.stderr)

    def test_valid_signed_beta_is_accepted(self):
        result = self.verify()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_legacy_apksigner_output_is_accepted(self):
        self.env["TEST_SIGNER_PREFIX"] = "Signer #1"
        self.env["TEST_SIGNER_SEPARATOR"] = ""
        result = self.verify()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_numbered_signer_with_colon_is_accepted(self):
        self.env["TEST_SIGNER_PREFIX"] = "Signer #1"
        result = self.verify()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_sdk_range_signer_labels_are_accepted(self):
        self.env["TEST_SIGNER_PREFIX"] = "Signer (minSdkVersion=26, maxSdkVersion=32)"
        for separator in ("", ":"):
            with self.subTest(separator=separator):
                self.env["TEST_SIGNER_SEPARATOR"] = separator
                result = self.verify()
                self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_every_distinct_signer_must_match(self):
        self.env["TEST_EXTRA_CERT"] = CERT
        result = self.verify()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.env["TEST_EXTRA_CERT"] = "0" * 40
        self.assertNotEqual(self.verify().returncode, 0)

    def test_missing_or_malformed_fingerprint_is_rejected(self):
        for value in ("", "certificate SHA-1 digest", CERT + "00"):
            with self.subTest(value=value):
                self.env["TEST_CERT"] = value
                self.assertNotEqual(self.verify().returncode, 0)
        self.env["TEST_NO_CERT"] = "true"
        self.assertNotEqual(self.verify().returncode, 0)

    def test_unsigned_or_invalid_signature_is_rejected_after_rename(self):
        self.env["TEST_SIGNATURE"] = "unsigned"
        result = self.verify()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("DOES NOT VERIFY", result.stderr)

    def test_wrong_signer_is_rejected(self):
        self.env["TEST_CERT"] = "0" * 40
        result = self.verify()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("expected", result.stdout)

    def test_wrong_identity_versions_or_permissions_are_rejected(self):
        for name, value in (
            ("TEST_PACKAGE", "com.other.app"),
            ("TEST_CODE", "1009000"),
            ("TEST_NAME", "1.14.0"),
            ("TEST_PERMISSION", "android.permission.POST_NOTIFICATIONS"),
        ):
            with self.subTest(name=name):
                previous = self.env[name]
                self.env[name] = value
                result = self.verify()
                self.env[name] = previous
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("::error::", result.stdout)

    def test_missing_apk_is_rejected(self):
        self.apk.unlink()
        self.assertNotEqual(self.verify().returncode, 0)

    def test_beta_carries_the_base_stable_version_code(self):
        # Design 1.15.0.D1: a 1.16.0 beta built on main 1.15.0 carries versionCode 1015000.
        self.env.update(TEST_CODE="1015000", TEST_NAME="1.16.0-beta.3")
        ok = self.run_script("verify_apk.sh", self.apk.as_posix(), "1.16.0-beta.3", "1.15.0")
        self.assertEqual(ok.returncode, 0, ok.stdout + ok.stderr)
        wrong = self.run_script("verify_apk.sh", self.apk.as_posix(), "1.16.0-beta.3", "1.16.0")
        self.assertNotEqual(wrong.returncode, 0)
        self.assertIn("versionCode='1016000'", wrong.stdout)

    def test_workflow_wires_the_beta_version_code(self):
        source = (ROOT / ".github/workflows/build.yml").read_text(encoding="utf-8")
        prepare = source.split("\n  prepare:\n", 1)[1].split("\n  windows:\n", 1)[0]
        android = source.split("\n  android:\n", 1)[1].split("\n  package:\n", 1)[0]
        package = source.split("\n  package:\n", 1)[1]
        self.assertIn('code_version="$base"', prepare)
        self.assertEqual(prepare.count('code_version="$version"'), 2)  # bootstrap + main
        self.assertIn('echo "code_version=$code_version" >> "$GITHUB_OUTPUT"', prepare)
        self.assertIn("code_version: ${{ steps.ver.outputs.code_version }}", prepare)
        self.assertIn('-PversionCodeOverride="$(python3 ../common/scripts/version.py code "${{ needs.prepare.outputs.code_version }}")"', android)
        for part in (android, package):
            self.assertIn('verify_apk.sh "$apk" "${{ needs.prepare.outputs.display }}" "${{ needs.prepare.outputs.code_version }}"', part)
            self.assertNotIn('"${{ needs.prepare.outputs.display }}" "${{ needs.prepare.outputs.version }}"', part)
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn('findProperty("versionCodeOverride")', gradle)
        self.assertIn("require(code in 1..appVersion.code)", gradle)

    def test_workflow_requires_verified_android_before_any_upload(self):
        source = (ROOT / ".github/workflows/build.yml").read_text(encoding="utf-8")
        android = source.split("\n  android:\n", 1)[1].split("\n  package:\n", 1)[0]
        package = source.split("\n  package:\n", 1)[1]
        self.assertIn("signed: ${{ steps.apk.outputs.signed }}", android)
        self.assertNotIn("app-release-unsigned.apk", source)
        self.assertLess(android.index("run: bash common/scripts/restore_signing_key.sh"), android.index("name: Build release APK"))
        self.assertLess(android.index("bash common/scripts/verify_apk.sh"), android.index('echo "signed=true"'))
        self.assertLess(android.index('echo "signed=true"'), android.index("uses: actions/upload-artifact"))
        self.assertIn("needs.android.result == 'success' && needs.android.outputs.signed == 'true'", package)
        self.assertLess(package.index("bash common/scripts/verify_apk.sh"), package.index("bash common/scripts/package.sh"))
        self.assertLess(package.index("bash common/scripts/package.sh"), package.index("uses: actions/upload-artifact"))
        self.assertIn("run: python3 common/scripts/test_release_signing.py", source)


if __name__ == "__main__":
    unittest.main(verbosity=2)
