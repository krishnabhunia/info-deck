#!/usr/bin/env python3
"""InfoDeck version rules — the single implementation CI uses.

Format x.y.z of plain integers (never decimals, never zero-padded): 2.9 -> 2.10 -> 2.11.
  x  major            — release:major -> (x+1).1, applied only by Krishna
  y  feature releases — release:feature  -> y+1, z dropped
  z  bug-fix releases — release:fix      -> z+1 (z is omitted until the first fix in a y)
  release:none        — docs/CI-only change: version unchanged, nothing published
versionCode = x*1,000,000 + y*1,000 + z

Usage:
  version.py next <current> <major|feature|fix|none>
  version.py code <version>
  version.py check <version>
"""
import re
import sys

PATTERN = re.compile(r"^(\d+)\.(\d+)(?:\.(\d+))?$")


def parse(v: str) -> tuple[int, int, int | None]:
    m = PATTERN.match(v.strip())
    if not m:
        raise ValueError(f"version must be x.y or x.y.z of plain integers, got {v!r}")
    x, y, z = m.groups()
    if (len(x) > 1 and x.startswith("0")) or (len(y) > 1 and y.startswith("0")) or (z and len(z) > 1 and z.startswith("0")):
        raise ValueError(f"version parts are never zero-padded: {v!r}")
    return int(x), int(y), (int(z) if z is not None else None)


def fmt(x: int, y: int, z: int | None) -> str:
    return f"{x}.{y}" if not z else f"{x}.{y}.{z}"


def next_version(current: str, kind: str) -> str:
    x, y, z = parse(current)
    if kind == "major":
        # same rule as a brand-new app (1.1): the first release of a major is x.1
        return fmt(x + 1, 1, None)
    if kind == "feature":
        return fmt(x, y + 1, None)
    if kind == "fix":
        return fmt(x, y, (z or 0) + 1)
    if kind == "none":
        return fmt(x, y, z)
    raise ValueError(f"unknown bump kind {kind!r} (major|feature|fix|none)")


def code(v: str) -> int:
    x, y, z = parse(v)
    if y >= 1000 or (z or 0) >= 1000:
        raise ValueError("y and z must stay below 1000")
    return x * 1_000_000 + y * 1_000 + (z or 0)


def _self_test() -> None:
    cases = [
        ("1.9", "feature", "1.10"), ("2.9", "feature", "2.10"), ("1.10", "fix", "1.10.1"),
        ("1.10.1", "fix", "1.10.2"), ("1.10.2", "feature", "1.11"), ("1.9", "none", "1.9"),
        ("1.9", "major", "2.1"),
    ]
    for cur, kind, want in cases:
        got = next_version(cur, kind)
        assert got == want, f"{cur} {kind}: {got} != {want}"
    assert code("1.9") == 1_009_000 and code("1.10.2") == 1_010_002
    for bad in ["1.09", "1", "v1.9", "1.9.0.1", "1.x"]:
        try:
            parse(bad)
        except ValueError:
            continue
        raise AssertionError(f"{bad} should be rejected")


if __name__ == "__main__":
    _self_test()
    try:
        cmd = sys.argv[1]
        if cmd == "next":
            print(next_version(sys.argv[2], sys.argv[3]))
        elif cmd == "code":
            print(code(sys.argv[2]))
        elif cmd == "check":
            parse(sys.argv[2]); print("ok")
        elif cmd == "selftest":
            print("version rules: all self-tests pass")
        else:
            raise IndexError
    except (IndexError, ValueError) as e:
        print(f"error: {e}" if isinstance(e, ValueError) else __doc__, file=sys.stderr)
        sys.exit(2)
