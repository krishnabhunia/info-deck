#!/usr/bin/env python3
"""Static regression checks for InfoDeck's approval-safe CI token flow."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT / ".github/workflows/build.yml"


def check_workflow() -> None:
    source = WORKFLOW.read_text(encoding="utf-8")
    assert source.count("\nname: Build\n") == 1
    assert "on:\n  pull_request:" in source
    assert "pull_request_target" not in source
    assert "persist-credentials: false" in source
    # Existing PR-version bot plus the manual release-request job.
    assert source.count("actions/create-github-app-token@v3") == 2
    assert "permission-pull-requests: write" in source
    assert "github.event_name == 'workflow_dispatch' && inputs.action == 'patch-pr'" in source
    assert "run: python3 common/scripts/test_patch_release.py" in source
    assert "permission-contents: write" in source
    assert "vars.INFODECK_CI_APP_CLIENT_ID" in source
    assert "secrets.INFODECK_CI_APP_PRIVATE_KEY" in source
    assert "github.event.pull_request.head.repo.full_name == github.repository" in source
    assert "steps.version-update.outputs.needed == 'true'" in source
    assert "gh auth setup-git" in source
    assert "GH_TOKEN: ${{ steps.ci-app.outputs.token }}" in source
    assert "GITHUB_TOKEN fallback is intentionally disabled" in source
    assert "run: python3 common/scripts/check_ci_token_workflow.py" in source
    docs = (ROOT / "docs/ci-app-token.md").read_text(encoding="utf-8")
    assert "only" in docs and "INFODECK_CI_APP_CLIENT_ID" in docs
    print("CI approval-safe app token checks passed")


if __name__ == "__main__":
    check_workflow()
