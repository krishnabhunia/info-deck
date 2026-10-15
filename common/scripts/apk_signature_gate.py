#!/usr/bin/env python3
"""Class-wide gate: no unsigned APK may ever leave CI.

Android refuses to install an APK that carries no signature and shows
"App not installed as package appears to be invalid". This gate makes that
failure impossible to ship: every APK the workflow hands to a person (the
Actions artifact or a GitHub release) must pass `check` first.

Usage:
    apk_signature_gate.py check <file.apk> [...]   # exit 1 if any APK is unsigned
    apk_signature_gate.py selftest                  # synthetic signed/unsigned fixtures
    apk_signature_gate.py workflow                  # build.yml wiring regression check

What it proves: the APK contains an APK Signature Scheme v2/v3/v3.1 block or
a v1 (JAR) signature. It does NOT verify the cryptography - `apksigner verify`
in common/scripts/check_cert.sh does that, and also pins the certificate.
Pure standard library, so it runs in any job (no Android SDK, no Java).
"""
from __future__ import annotations

import contextlib
import io
import re
import struct
import sys
import tempfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT / ".github/workflows/build.yml"

SIG_BLOCK_MAGIC = b"APK Sig Block 42"
EOCD_SIG = b"PK\x05\x06"
SCHEME_IDS = {0x7109871A: "v2", 0xF05368C0: "v3", 0x1B93AD61: "v3.1"}
V1_SIG_FILE = re.compile(r"^META-INF/[^/]+\.SF$", re.IGNORECASE)
V1_BLOCK_FILE = re.compile(r"^META-INF/[^/]+\.(RSA|DSA|EC)$", re.IGNORECASE)
INVALID_HINT = ('Android would reject it with "App not installed as package appears to be invalid". '
                "Add the signing secrets - docs/release-process.md section 1.")


class GateError(Exception):
    """The file is not an APK this gate can accept."""


def _central_directory_offset(data: bytes) -> int:
    # EOCD is 22 bytes plus a comment of at most 65535 bytes.
    start = max(0, len(data) - 22 - 0xFFFF)
    pos = data.rfind(EOCD_SIG, start)
    if pos < 0 or pos + 22 > len(data):
        raise GateError("not a ZIP/APK (no end-of-central-directory record)")
    (cd_offset,) = struct.unpack_from("<I", data, pos + 16)
    if cd_offset > pos:
        raise GateError("corrupt ZIP (central directory offset past end)")
    return cd_offset


def v2_plus_schemes(data: bytes) -> list[str]:
    """Signature schemes found in the APK Signing Block that sits right before the central directory."""
    cd = _central_directory_offset(data)
    if cd < 24 or data[cd - 16:cd] != SIG_BLOCK_MAGIC:
        return []
    (size_footer,) = struct.unpack_from("<Q", data, cd - 24)
    block_start = cd - size_footer - 8
    if size_footer < 24 or block_start < 0:
        raise GateError("corrupt APK Signing Block size")
    (size_header,) = struct.unpack_from("<Q", data, block_start)
    if size_header != size_footer:
        raise GateError("APK Signing Block header/footer sizes disagree")
    found: list[str] = []
    pos, end = block_start + 8, cd - 24
    while pos < end:
        if pos + 12 > end:
            raise GateError("truncated APK Signing Block entry")
        (length,) = struct.unpack_from("<Q", data, pos)
        if length < 4 or pos + 8 + length > end:
            raise GateError("bad APK Signing Block entry length")
        (block_id,) = struct.unpack_from("<I", data, pos + 8)
        if block_id in SCHEME_IDS and length > 4:
            found.append(SCHEME_IDS[block_id])
        pos += 8 + length
    return found


def has_v1(data: bytes) -> bool:
    try:
        names = zipfile.ZipFile(io.BytesIO(data)).namelist()
    except zipfile.BadZipFile as exc:
        raise GateError(f"not a ZIP/APK ({exc})") from exc
    return any(V1_SIG_FILE.match(n) for n in names) and any(V1_BLOCK_FILE.match(n) for n in names)


def schemes(data: bytes) -> list[str]:
    found = v2_plus_schemes(data)
    if has_v1(data):
        found.insert(0, "v1")
    return found


def check(paths: list[str]) -> int:
    if not paths:
        print("::error::apk_signature_gate: no APK given")
        return 1
    failed = 0
    for p in paths:
        path = Path(p)
        try:
            if not path.is_file():
                raise GateError("file not found")
            found = schemes(path.read_bytes())
        except GateError as exc:
            print(f"::error::{path.name}: {exc}. {INVALID_HINT}")
            failed += 1
            continue
        if not found:
            print(f"::error::{path.name} is UNSIGNED (no v1/v2/v3 signature). {INVALID_HINT}")
            failed += 1
        else:
            print(f"{path.name}: signed ({', '.join(found)})")
    return 1 if failed else 0


# ------------------------------------------------------------------ self-test
def _zip_bytes(entries: dict[str, bytes]) -> bytes:
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w", compression=zipfile.ZIP_DEFLATED) as z:
        for name, body in entries.items():
            z.writestr(name, body)
    return buf.getvalue()


def _with_sig_block(apk: bytes, pairs: list[tuple[int, bytes]]) -> bytes:
    cd = _central_directory_offset(apk)
    body = b"".join(struct.pack("<QI", 4 + len(v), i) + v for i, v in pairs)
    size = len(body) + 8 + 16
    block = struct.pack("<Q", size) + body + struct.pack("<Q", size) + SIG_BLOCK_MAGIC
    eocd = apk.rfind(EOCD_SIG)
    fixed_eocd = apk[eocd:eocd + 16] + struct.pack("<I", cd + len(block)) + apk[eocd + 20:]
    return apk[:cd] + block + apk[cd:eocd] + fixed_eocd


def selftest() -> None:
    base = {"AndroidManifest.xml": b"\x03\x00manifest", "classes.dex": b"dex\n035\x00" + b"\x00" * 64}
    unsigned = _zip_bytes(base)
    assert schemes(unsigned) == [], "unsigned APK must have no schemes"

    v1 = _zip_bytes({**base, "META-INF/MANIFEST.MF": b"m", "META-INF/CERT.SF": b"s", "META-INF/CERT.RSA": b"r"})
    assert schemes(v1) == ["v1"]
    sf_only = _zip_bytes({**base, "META-INF/CERT.SF": b"s"})
    assert schemes(sf_only) == [], ".SF without a signature block file is not a v1 signature"

    v2 = _with_sig_block(unsigned, [(0x7109871A, b"signer")])
    assert schemes(v2) == ["v2"], schemes(v2)
    assert zipfile.ZipFile(io.BytesIO(v2)).read("AndroidManifest.xml") == base["AndroidManifest.xml"]
    v2v3 = _with_sig_block(unsigned, [(0x7109871A, b"a"), (0xF05368C0, b"b")])
    assert schemes(v2v3) == ["v2", "v3"]
    padding_only = _with_sig_block(unsigned, [(0x42726577, b"\x00" * 16)])
    assert schemes(padding_only) == [], "a signing block with only padding is not a signature"

    # Run the negative cases silently: their ::error:: lines would otherwise show up
    # as red annotations on a passing CI step.
    with tempfile.TemporaryDirectory() as tmp, contextlib.redirect_stdout(io.StringIO()):
        t = Path(tmp)
        (t / "unsigned.apk").write_bytes(unsigned)
        (t / "v2.apk").write_bytes(v2)
        (t / "junk.apk").write_bytes(b"not a zip at all")
        assert check([str(t / "v2.apk")]) == 0
        assert check([str(t / "unsigned.apk")]) == 1
        assert check([str(t / "junk.apk")]) == 1
        assert check([str(t / "missing.apk")]) == 1
        assert check([str(t / "v2.apk"), str(t / "unsigned.apk")]) == 1, "one bad APK fails the batch"
        assert check([]) == 1
    print("APK signature gate self-test passed")


# ------------------------------------------------------------------ wiring
def workflow() -> None:
    """build.yml must never be able to hand out an unsigned APK again."""
    src = WORKFLOW.read_text(encoding="utf-8")
    assert "app-release-unsigned.apk" not in src, "workflow must never pick up the unsigned APK"
    assert "run: python3 common/scripts/apk_signature_gate.py selftest" in src
    assert "run: python3 common/scripts/apk_signature_gate.py workflow" in src
    # Gate before the APK leaves the Android job, and again before it goes into the zip/release.
    assert src.count("python3 common/scripts/apk_signature_gate.py check") >= 2
    assert src.count("bash common/scripts/check_cert.sh") == 1
    # All three secrets are required; a missing one stops the job after the unit tests.
    for name in ("INFODECK_KEYSTORE_B64", "INFODECK_STOREPASS", "INFODECK_KEYPASS"):
        assert f"secrets.{name}" in src, name
    assert "Stop - signing secrets missing" in src
    android = src.split("  android:\n", 1)[1].split("\n  package:\n", 1)[0]
    stop = android.index("Stop - signing secrets missing")
    assert android.index("Unit tests") < stop < android.index("Build release APK"), \
        "tests must still run, and the build must not start, when secrets are missing"
    assert android.index("apk_signature_gate.py check") < android.index("actions/upload-artifact"), \
        "the gate must run before the APK is uploaded"
    package = src.split("\n  package:\n", 1)[1]
    assert package.index("apk_signature_gate.py check") < package.index("bash common/scripts/package.sh")
    print("APK signing workflow wiring checks passed")


def main(argv: list[str]) -> int:
    if len(argv) >= 2 and argv[1] == "check":
        return check(argv[2:])
    if argv[1:] == ["selftest"]:
        selftest()
        return 0
    if argv[1:] == ["workflow"]:
        workflow()
        return 0
    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
