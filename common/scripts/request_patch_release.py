#!/usr/bin/env python3
"""Open a patch-version PR from main; never push to main or create a release.

Called only by the manually dispatched, main-only GitHub Actions job.
The dedicated repository-scoped GitHub App provides GH_TOKEN.
"""
from __future__ import annotations

import os
from pathlib import Path
import subprocess
import sys

from version import code, next_version, parse


class PatchReleaseError(RuntimeError):
    """A patch request could not be safely completed."""


def run(*args: str, check: bool = True) -> subprocess.CompletedProcess[str]:
    result = subprocess.run(args, text=True, capture_output=True, check=False)
    if check and result.returncode:
        # Print a sanitized command name, not credentials or command arguments.
        raise PatchReleaseError(
            f"{args[0]} operation failed (exit {result.returncode}): "
            f"{result.stderr.strip() or result.stdout.strip()}"
        )
    return result


def remote_ref_exists(ref: str) -> bool:
    result = run("git", "ls-remote", "--exit-code", "origin", ref, check=False)
    if result.returncode not in (0, 2):
        raise PatchReleaseError(f"Could not check remote Git ref (exit {result.returncode})")
    return result.returncode == 0


def patch_plan(main_version: str) -> tuple[str, str]:
    """Use strict main x.y.z, and the existing repository version rules."""
    current = main_version.strip()
    parse(current)
    target = next_version(current, "fix")
    code(target)  # Reject Android versionCode overflow before any Git write.
    return target, f"cgpt-release-patch-v{target}"


def request_patch_pr() -> str:
    if os.environ.get("GITHUB_REF") != "refs/heads/main":
        raise PatchReleaseError("Run this workflow on main only; no PR was created.")
    if not os.environ.get("GH_TOKEN"):
        raise PatchReleaseError("The scoped GitHub App token is required; no fallback token is allowed.")

    run("git", "fetch", "--quiet", "origin", "+refs/heads/main:refs/remotes/origin/main")
    base = run("git", "rev-parse", "refs/remotes/origin/main").stdout.strip()
    current = run("git", "show", "refs/remotes/origin/main:VERSION").stdout.strip()
    target, branch = patch_plan(current)
    tag = f"refs/tags/v{target}"

    if remote_ref_exists(tag):
        raise PatchReleaseError(f"v{target} already has a release tag. Refresh main before retrying.")

    existing = run(
        "gh", "pr", "list", "--state", "open", "--base", "main",
        "--head", branch, "--json", "url", "--jq", ".[0].url // empty"
    ).stdout.strip()
    if existing:
        print(f"An open PR already requests v{target}: {existing}")
        return existing
    if remote_ref_exists(f"refs/heads/{branch}"):
        raise PatchReleaseError(
            f"Branch {branch} already exists without an open PR; review it manually. "
            "The bot will not overwrite an existing branch."
        )

    # Abort rather than deliberately use a stale baseline if main has moved.
    latest = run("git", "ls-remote", "origin", "refs/heads/main").stdout.split()
    if not latest or latest[0] != base:
        raise PatchReleaseError("main moved during the request; run the action again.")

    run("git", "switch", "--detach", base)
    run("git", "switch", "--create", branch)
    Path("VERSION").write_text(target + "\n", encoding="utf-8")
    run("git", "add", "--", "VERSION")
    run(
        "git", "-c", "user.name=InfoDeck Version Bot",
        "-c", "user.email=41898282+github-actions[bot]@users.noreply.github.com",
        "commit", "-m", f"chore: request patch release v{target}"
    )
    run("gh", "auth", "setup-git")
    run("git", "push", "origin", f"HEAD:refs/heads/{branch}")

    title = f"cgpt-release-patch-v{target}"
    body = (
        f"## One-click patch release request\n\n"
        f"- Base: main at {base}\n"
        f"- Version: {current} -> {target} (z + 1)\n"
        "- Changes: VERSION only; no source changes or direct stable release.\n"
        "- Label: release:fix; PR checks build a signed beta when secrets are configured.\n\n"
        "## Before merging\n"
        "- [ ] Confirm this patch release is appropriate for fixes already on main.\n"
        "- [ ] Review beta APK installation and original certificate signing.\n"
        "- [ ] Update release date/changelog/backlog if applicable (docs/release-process.md).\n"
        "- [ ] Review and merge normally; the main workflow then publishes the stable release.\n\n"
        "Created by a manual GitHub Actions dispatch; no automatic merge."
    )
    url = run(
        "gh", "pr", "create", "--base", "main", "--head", branch,
        "--title", title, "--body", body
    ).stdout.strip()
    if not url.startswith("https://github.com/"):
        raise PatchReleaseError("GitHub did not return a valid PR URL; inspect the pushed branch.")
    run("gh", "pr", "edit", url, "--add-label", "release:fix")
    print(f"Created {url} for v{target}. Review before merging.")
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a", encoding="utf-8") as summary:
            summary.write(f"### Patch release requested\n\n- **v{target}**: {url}\n- Review and merge manually.\n")
    return url


if __name__ == "__main__":
    try:
        request_patch_pr()
    except (PatchReleaseError, ValueError, OSError) as error:
        # Do not log GH_TOKEN, GitHub App private key, or other environment values.
        message = str(error).replace("\n", " ").replace("\r", " ")
        print(f"::error::{message}", file=sys.stderr)
        sys.exit(1)
