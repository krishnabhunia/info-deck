#!/usr/bin/env python3
"""InfoDeck versioning: releases always use x.y.z, with no zero-padding.

An existing two-part baseline is accepted ONLY by "next" for migration.
major -> (x+1).1.0; feature -> x.(y+1).0; fix -> x.y.(z+1);
none -> x.y.z; versionCode = x*1_000_000+y*1_000+z.
"""
import re
import sys

PATTERN = re.compile(r"^([0-9]+)\.([0-9]+)\.([0-9]+)$")
LEGACY = re.compile(r"^([0-9]+)\.([0-9]+)$")


def parse(v: str, *, allow_legacy: bool = False) -> tuple[int, int, int]:
    m = PATTERN.fullmatch(v)
    if m is None and allow_legacy:
        m = LEGACY.fullmatch(v)
    if m is None:
        raise ValueError(f"version must be x.y.z of plain integers, got {v!r}")
    parts = m.groups()
    if any(len(p) > 1 and p.startswith("0") for p in parts):
        raise ValueError(f"zero-padded version component: {v!r}")
    return int(parts[0]), int(parts[1]), int(parts[2]) if len(parts) == 3 else 0


def fmt(x: int, y: int, z: int) -> str:
    return f"{x}.{y}.{z}"


def next_version(current: str, kind: str) -> str:
    x, y, z = parse(current, allow_legacy=True)
    if kind == "major":
        return fmt(x + 1, 1, 0)
    if kind == "feature":
        return fmt(x, y + 1, 0)
    if kind == "fix":
        return fmt(x, y, z + 1)
    if kind == "none":
        return fmt(x, y, z)
    raise ValueError(f"unknown bump kind {kind!r}")


def code(v: str) -> int:
    x, y, z = parse(v)
    if y >= 1000 or z >= 1000:
        raise ValueError("y and z must stay below 1000")
    return x * 1_000_000 + y * 1_000 + z


def _self_test() -> None:
    cases = [
        ("1.11", "feature", "1.12.0"),
        ("1.9.0", "feature", "1.10.0"),
        ("2.9.0", "feature", "2.10.0"),
        ("1.10.0", "fix", "1.10.1"),
        ("1.10.1", "fix", "1.10.2"),
        ("1.10.2", "feature", "1.11.0"),
        ("1.11.0", "none", "1.11.0"),
        ("1.11.0", "major", "2.1.0"),
    ]
    for cur, kind, want in cases:
        actual = next_version(cur, kind)
        assert actual == want, (cur, kind, actual, want)
    assert code("1.9.0") == 1_009_000
    assert code("1.10.2") == 1_010_002
    for bad in ("1.11", "1", "v1.9.0", "1.9.0.1", "1.x.0", "1.09.0", "1.9.00", "1.9.-1", "١.٢.٣"):
        try:
            parse(bad)
        except ValueError:
            continue
        raise AssertionError(f"{bad!r} should be rejected")
    for bad in ("1.11", "1.11.00"):
        try:
            code(bad)
        except ValueError:
            continue
        raise AssertionError(f"{bad!r} should be rejected by versionCode")


if __name__ == "__main__":
    _self_test()
    try:
        cmd = sys.argv[1]
        if cmd == "next":
            print(next_version(sys.argv[2], sys.argv[3]))
        elif cmd == "code":
            print(code(sys.argv[2]))
        elif cmd == "check":
            parse(sys.argv[2])
            print("ok")
        elif cmd == "selftest":
            print("version rules: all self-tests pass")
        else:
            raise IndexError
    except (ValueError, IndexError) as e:
        print(f"error: {e}" if isinstance(e, ValueError) else __doc__, file=sys.stderr)
        sys.exit(2)
