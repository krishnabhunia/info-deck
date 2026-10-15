#!/usr/bin/env python3
"""Regression tests for manual patch-release PR safety; no network or credentials."""
from __future__ import annotations

import os
from pathlib import Path
import subprocess
import unittest
from unittest.mock import patch

import request_patch_release as release


ROOT = Path(__file__).resolve().parents[2]


class PatchReleaseTests(unittest.TestCase):
    def test_version_is_calculated_not_hardcoded(self):
        self.assertEqual(release.patch_plan("1.14.0"), ("1.14.1", "cgpt-release-patch-v1.14.1"))
        self.assertEqual(release.patch_plan("1.14.8"), ("1.14.9", "cgpt-release-patch-v1.14.9"))
        self.assertEqual(release.patch_plan("1.9.9"), ("1.9.10", "cgpt-release-patch-v1.9.10"))

    def test_invalid_or_legacy_main_versions_rejected(self):
        for bad in ("1.14", "v1.14.0", "1.014.0", "1.2.1000", ""):
            with self.subTest(bad=bad), self.assertRaises(ValueError):
                release.patch_plan(bad)

    def test_non_main_ref_rejected_before_git_or_github(self):
        with patch.dict(os.environ, {"GITHUB_REF": "refs/heads/feature", "GH_TOKEN": "fake"}):
            with patch.object(release, "run") as invoke:
                with self.assertRaisesRegex(release.PatchReleaseError, "main only"):
                    release.request_patch_pr()
                invoke.assert_not_called()

    def test_missing_app_token_fails_without_fallback(self):
        with patch.dict(os.environ, {"GITHUB_REF": "refs/heads/main"}, clear=True):
            with patch.object(release, "run") as invoke:
                with self.assertRaisesRegex(release.PatchReleaseError, "App token"):
                    release.request_patch_pr()
                invoke.assert_not_called()

    def test_existing_patch_pr_does_not_write_or_push(self):
        history = []

        def fake_run(*args, **kwargs):
            history.append(args)
            if args[:3] == ("git", "rev-parse", "refs/remotes/origin/main"):
                output = "a" * 40
            elif args[:3] == ("git", "show", "refs/remotes/origin/main:VERSION"):
                output = "1.14.0\n"
            elif args[:3] == ("git", "ls-remote", "--exit-code"):
                return subprocess.CompletedProcess(args, 2, "", "")
            elif args[:3] == ("gh", "pr", "list"):
                output = "https://github.com/krishnabhunia/info-deck/pull/100\n"
            else:
                output = ""
            return subprocess.CompletedProcess(args, 0, output, "")

        with patch.dict(os.environ, {"GITHUB_REF": "refs/heads/main", "GH_TOKEN": "fake"}):
            with patch.object(release, "run", side_effect=fake_run):
                result = release.request_patch_pr()
        self.assertTrue(result.endswith("/pull/100"))
        self.assertFalse(any(args[:2] == ("git", "push") for args in history))
        self.assertFalse(any(args[:3] == ("gh", "pr", "create") for args in history))

    def test_branch_name_collision_cannot_be_overwritten(self):
        with patch.object(release, "remote_ref_exists", side_effect=[False, True]):
            def fake_run(*args, **kwargs):
                output = {
                    ("git", "rev-parse", "refs/remotes/origin/main"): "a" * 40,
                    ("git", "show", "refs/remotes/origin/main:VERSION"): "1.14.0",
                }.get(args, "")
                return subprocess.CompletedProcess(args, 0, output, "")
            with patch.dict(os.environ, {"GITHUB_REF": "refs/heads/main", "GH_TOKEN": "fake"}):
                with patch.object(release, "run", side_effect=fake_run) as invoke:
                    with self.assertRaisesRegex(release.PatchReleaseError, "already exists"):
                        release.request_patch_pr()
                    self.assertFalse(any(call.args[:2] == ("git", "push") for call in invoke.call_args_list))

    def test_one_workflow_and_main_only_pr_path(self):
        yaml = (ROOT / ".github/workflows/build.yml").read_text(encoding="utf-8")
        self.assertEqual(len(list((ROOT / ".github/workflows").glob("*.yml"))), 1)
        self.assertIn("workflow_dispatch:", yaml)
        self.assertIn("patch-pr", yaml)
        self.assertIn("github.event_name != 'workflow_dispatch' || inputs.action != 'patch-pr'", yaml)
        self.assertIn("python3 common/scripts/request_patch_release.py", yaml)
        self.assertIn("permission-pull-requests: write", yaml)
        self.assertIn("run: python3 common/scripts/test_patch_release.py", yaml)
        self.assertNotIn("pull_request_target", yaml)


if __name__ == "__main__":
    unittest.main(verbosity=2)
