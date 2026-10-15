# GitHub App token for automatic PR version commits

This repository uses a single workflow ([build.yml](../.github/workflows/build.yml)).
GitHub now puts `pull_request/synchronize` runs triggered by the default
`GITHUB_TOKEN` into an **approval-required** state, including when that token
pushes a `VERSION` commit to a PR created by the repository owner.

## One-time setup (repository owner)

Create a **dedicated GitHub App** (not a PAT; not the Android signing keystore):

1. Open [GitHub App settings](https://github.com/settings/apps/new).
   Name the app (for example, `InfoDeck Version Bot`) and disable webhooks
   unless you actually need them.
2. Under **Repository permissions**, give **Contents: Read and write**.
   Do not grant Actions, Administration, Secrets, or other privileged
   permissions. GitHub automatically supplies Metadata: Read.
3. Generate a **GitHub App private key** (PEM). Keep it offline and private.
4. Install the App on the account that owns InfoDeck and grant access to
   **only** `krishnabhunia/info-deck`. If repository rules prevent that
   App from updating PR branches, the owner must adjust the applicable
   rules explicitly without bypassing `main` branch protections.
5. Go to [Actions variables](https://github.com/krishnabhunia/info-deck/settings/variables/actions).
   Add the repository **variable** `INFODECK_CI_APP_CLIENT_ID` containing
   the App's Client ID (the `Iv...` identifier displayed by GitHub).
6. Go to [Actions secrets](https://github.com/krishnabhunia/info-deck/settings/secrets/actions).
   Add `INFODECK_CI_APP_PRIVATE_KEY` containing the full PEM text from
   **that GitHub App**. Never commit, print, or attach the private key.

**Do not use `infodeck.keystore`, the APK signing key, or any Android
password here.** The GitHub App PEM only authorizes automated version
commits; the APK signing credentials are unrelated and remain separate.

## What changes after setup

- On a same-repository PR with exactly one `release:*` label, the Prepare
  job calculates the next `x.y.z` version using `version.py`.
- If the PR branch `VERSION` already matches, nothing is pushed. This
  includes normal `release:none` PRs.
- If it differs, a short-lived, **repository-scoped GitHub App** token is
  generated with **Contents: Write** only. The App pushes exactly the
  `VERSION` commit, and GitHub can run the new PR workflow without the
  `GITHUB_TOKEN` approval banner.
- Fork PRs **never** receive the App token and never get a version push;
  this workflow intentionally does not use `pull_request_target`.
- If configuration is missing when a bump is needed, Prepare fails clearly.
  It **does not fall back** to `GITHUB_TOKEN`, since that would recreate
  the approval problem and mislead the maintainer.
- CI always uses the computed version for beta packaging; stable releases
  continue to use `VERSION` on `main` after merging.

## Verification

Run `python3 common/scripts/check_ci_token_workflow.py`.

1. Merge this CI-only PR with `release:none`; its own head VERSION should
   already equal main, so no GitHub App setup is required just to validate
   the change.
2. Configure the App as above.
3. Create a **new feature PR** with its VERSION inherited from main and
   apply `release:feature` (or a fix PR with `release:fix`).
4. Observe a `Bump VERSION to ...` commit authored by your dedicated
   GitHub App and a successful automatic follow-up workflow run **without**
   `Approve workflows to run`.
5. Confirm the APK/package test and secret scan pass. Leave GitHub's
   review/approval policy for outside contributors enabled.

Previously created `action_required` runs may remain in the PR history.
This configuration does not retroactively approve them or turn them green.

**Security:** Do not automatically approve arbitrary workflow runs using
a cron job, PAT, or privileged `pull_request_target` job. That weakens
GitHub's safeguard against execution of untrusted contributor code.
