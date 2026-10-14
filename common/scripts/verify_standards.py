#!/usr/bin/env python3
"""CI smoke checks against docs/standards/{github,workflows,versions_management}.md.

Runs without third-party packages. Invoked after CI computes the candidate x.y.z
version, including on the one-time migration PR whose base still uses x.y.
"""
from __future__ import annotations

import argparse
import importlib.util
import subprocess
import tempfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def check(version: str) -> None:
    spec = importlib.util.spec_from_file_location("info_deck_version", ROOT / "common/scripts/version.py")
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    module._self_test()
    assert module.parse(version) and module.fmt(*module.parse(version)) == version
    assert module.code(version) > 0

    # github.md: fixed platform roots and a common scripts location.
    for name in ("docs", "windows", "macOS", "android", "common", "common/scripts", "common/src"):
        assert (ROOT / name).is_dir(), f"Missing standard folder: {name}"
    for name in ("github.md", "workflows.md", "versions_management.md"):
        assert (ROOT / "docs/standards" / name).is_file(), f"Missing standard: {name}"

    # workflows.md: exactly one workflow for the three supported platforms.
    workflows = sorted((ROOT / ".github/workflows").glob("*.yml"))
    workflows += sorted((ROOT / ".github/workflows").glob("*.yaml"))
    assert len(workflows) == 1 and workflows[0].name == "build.yml", workflows
    workflow = workflows[0].read_text(encoding="utf-8")
    for job in ("  windows:", "  macos:", "  android:", "  package:"):
        assert job in workflow, f"Missing workflow job: {job}"
    assert "release:none" in workflow and "release:feature" in workflow

    # workflows.md: prove the packaging shell script creates the EXACT folder/
    # file names, both when platforms are present and when only Android exists.
    with tempfile.TemporaryDirectory(prefix="infodeck-standards-") as folder:
        temp = Path(folder)
        sources = [temp / n for n in ("source.apk", "x64.exe", "portable.exe", "mac.dmg")]
        for i, path in enumerate(sources):
            path.write_bytes(bytes([i + 1]))
        packer = ROOT / "common/scripts/package.sh"
        base = "InfoDeck_" + version

        def invoke(target: str, inputs: list[str]) -> list[str]:
            stage = temp / target
            subprocess.run(
                ["bash", str(packer), "InfoDeck", version, str(stage), *inputs],
                check=True, capture_output=True, text=True
            )
            zip_path = temp / (target + ".zip")
            with zipfile.ZipFile(zip_path, "w", compression=zipfile.ZIP_DEFLATED) as z:
                for f in stage.rglob("*"):
                    if f.is_file():
                        z.write(f, f.relative_to(stage))
            with zipfile.ZipFile(zip_path) as z:
                return sorted(z.namelist())

        assert invoke("android-only", [str(sources[0])]) == [f"Android/{base}.apk"]
        all_files = invoke("all-platforms", [str(p) for p in sources])
        assert all_files == sorted([
            f"Android/{base}.apk", f"portable/{base}.exe",
            f"windows-x64/{base}.exe", f"macOS/{base}.dmg"
        ]), all_files

    print(f"Repository standards smoke checks passed for {version}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--version", required=True, help="CI-computed stable x.y.z")
    args = parser.parse_args()
    check(args.version)
