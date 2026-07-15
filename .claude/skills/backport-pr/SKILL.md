---
name: backport-pr
description: Backport commits from an existing GitHub Pull Request to a target branch and open a new backport PR. Use when the user asks to backport a PR, cherry-pick a merged PR into a release/maintenance branch, port a fix to an older version, or mentions "backport".
user_invocable: true
---

# Backport PR Skill

**Purpose:** Given an existing GitHub Pull Request number, cherry-pick its commit(s) onto a target branch (e.g. `release/5.x`, `maintenance/4.x`) and open a new backport PR referencing the original.

**When to use:**
- Porting a merged fix/feature from `main` to a release branch
- Re-applying a patch from one long-lived branch onto another
- User mentions "backport", "cherry-pick PR", or "port fix to <branch>"

---

## Prerequisites

Before starting, verify:

1. `gh` CLI installed and authenticated:
   ```bash
   gh auth status
   ```
2. Current repo is a valid git clone with `origin` pointing at the GitHub repo.
3. Working tree is clean (no uncommitted changes).
4. You have push access to the `origin` remote (needed to open the backport PR).

If any check fails, stop and report the issue to the user before proceeding.

---

## Inputs

The skill takes one required input:

- **PR number** — the numeric GitHub PR being backported (e.g. `1234`).

Accept it from the invocation argument or prompt for it:
```
Enter the PR number to backport:
```

The **target branch** is always prompted (see Step 4).

---

## Workflow

Copy this checklist and track progress:

```
Backport Progress:
- [ ] Step 1: Validate environment
- [ ] Step 2: Fetch PR metadata
- [ ] Step 3: Determine commits to cherry-pick (merge strategy)
- [ ] Step 4: Prompt for target branch
- [ ] Step 5: Create backport branch
- [ ] Step 6: Cherry-pick commit(s)
- [ ] Step 7: Push backport branch
- [ ] Step 8: Open backport PR
- [ ] Step 9: Final summary
```

---

### Step 1: Validate Environment

```bash
gh auth status
git rev-parse --is-inside-work-tree
git status --short
```

- If `gh auth status` fails → tell the user to run `gh auth login` and stop.
- If `git status --short` shows any output → ask the user to commit or stash first and stop.

Also capture the remote repo slug for later PR operations:
```bash
gh repo view --json nameWithOwner -q .nameWithOwner
```

---

### Step 2: Fetch PR Metadata

```bash
gh pr view <pr-number> --json number,title,state,mergedAt,mergeCommit,commits,baseRefName,headRefName,url,author
```

Extract and store:
- `number` → `$PR_NUMBER`
- `title` → `$PR_TITLE`
- `state` → must be `MERGED` (warn if not; allow override)
- `mergeCommit.oid` → `$MERGE_SHA` (null indicates a **rebase-merged** PR; present for squash or merge-commit)
- `commits[*].oid` → `$PR_COMMITS` (ordered list)
- `baseRefName` → `$ORIGINAL_BASE` (usually `main`)
- `headRefName` → `$ORIGINAL_HEAD` (original source branch)
- `url` → `$PR_URL`

Display a short summary to the user:
```
PR #<number>: <title>
  Author:      <author.login>
  State:       <state>
  Base:        <baseRefName>
  Head:        <headRefName>
  Commits:     <len(commits)>
  Merge SHA:   <mergeCommit.oid or "none (likely rebase-merged)">
```

If state is not `MERGED`, ask:
```
This PR is not yet merged. Backport anyway? (yes/no)
```

---

### Step 3: Determine Commits to Cherry-Pick (Auto-Detect Merge Strategy)

Fetch the latest refs first:
```bash
git fetch origin --tags
```

Auto-detect which commit(s) to cherry-pick based on how the PR was merged:

1. **Squash merge** — `mergeCommit.oid` points to a commit on `$ORIGINAL_BASE` whose
   parent count is 1 AND whose commit message typically contains `(#<pr-number>)`.
   Check with:
   ```bash
   git cat-file -p <MERGE_SHA> | head -20
   # parents count:
   git rev-list --parents -n 1 <MERGE_SHA> | awk '{print NF-1}'
   ```
   - Parent count `1` → **squash**. Cherry-pick strategy: `git cherry-pick <MERGE_SHA>`.
   - Store: `CP_MODE=squash`, `CP_ARGS="<MERGE_SHA>"`.

2. **Merge commit** — `mergeCommit.oid` has 2 parents.
   - Cherry-pick strategy: `git cherry-pick -m 1 <MERGE_SHA>`.
   - Store: `CP_MODE=merge`, `CP_ARGS="-m 1 <MERGE_SHA>"`.

3. **Rebase merge** — `mergeCommit.oid` is null OR the commits appear individually on
   `$ORIGINAL_BASE` (no merge/squash commit). Also falls here if PR is not merged yet.
   - Cherry-pick strategy: cherry-pick every SHA in `$PR_COMMITS` in order.
   - Store: `CP_MODE=rebase`, `CP_ARGS="<sha1> <sha2> ..."`.

Always confirm the detected plan with the user before proceeding:
```
Detected merge strategy: <CP_MODE>
Will cherry-pick: <CP_ARGS>

Proceed? (yes/no/override)
```

If `override`, offer the three modes explicitly and let the user pick.

---

### Step 4: Prompt for Target Branch

Ask the user (always — do not guess):
```
Enter the target branch to backport into (e.g. release/5.x, maintenance/4.x):
```

Store as `$TARGET_BRANCH`.

Verify the target exists on `origin`:
```bash
git ls-remote --heads origin <TARGET_BRANCH>
```

If empty → error out with: `Target branch '<TARGET_BRANCH>' not found on origin.`

Fetch the target:
```bash
git fetch origin <TARGET_BRANCH>
```

---

### Step 5: Create Backport Branch

Compute branch name: `backport/<target-sanitized>/pr-<pr-number>`

Where `<target-sanitized>` replaces `/` with `-` (e.g. `release/5.x` → `release-5.x`).

Examples:
- Target `release/5.x`, PR `1234` → `backport/release-5.x/pr-1234`
- Target `maintenance/4.x`, PR `987` → `backport/maintenance-4.x/pr-987`

Show the user:
```
Backport branch name: backport/<target-sanitized>/pr-<pr-number>

Accept this name or provide an override? (accept/override)
```

Create the branch from the target:
```bash
git checkout -b backport/<target-sanitized>/pr-<pr-number> origin/<TARGET_BRANCH>
```

If the local branch already exists → ask:
```
Branch already exists. (delete-and-recreate/use-existing/abort)
```

---

### Step 6: Cherry-Pick Commit(s)

Execute according to `CP_MODE`:

**squash / merge:**
```bash
git cherry-pick $CP_ARGS
```

**rebase (multiple commits):** iterate in order:
```bash
git cherry-pick <sha1>
git cherry-pick <sha2>
...
```

For each cherry-pick, check exit code:

- **Success (exit 0)** → continue to next commit (if any).
- **Conflict (exit non-zero)** → pause and prompt the user:
  ```
  ⚠️ Cherry-pick conflict on <sha>

  Conflicted files:
  <git diff --name-only --diff-filter=U output>

  Options:
  1. Pause — I'll resolve the conflict manually, then you continue
  2. Abort — run `git cherry-pick --abort` and delete the backport branch
  3. Skip — run `git cherry-pick --skip` and move on (rebase mode only)

  Choose (1-3):
  ```

  - **Option 1 (pause):** wait for user to resolve, then run:
    ```bash
    git status
    ```
    Confirm no unmerged paths remain, then:
    ```bash
    git cherry-pick --continue
    ```
  - **Option 2 (abort):**
    ```bash
    git cherry-pick --abort
    # Switch to a safe branch before deleting the backport branch.
    # Use the local target branch if it exists; otherwise create it tracking origin.
    git show-ref --verify --quiet refs/heads/<TARGET_BRANCH> \
      && git checkout <TARGET_BRANCH> \
      || git checkout -b <TARGET_BRANCH> --track origin/<TARGET_BRANCH>
    git branch -D backport/<target-sanitized>/pr-<pr-number>
    ```
    Stop the skill and report failure.
  - **Option 3 (skip):** only valid in rebase mode with multiple commits:
    ```bash
    git cherry-pick --skip
    ```

After all commits are applied, verify:
```bash
git log --oneline origin/<TARGET_BRANCH>..HEAD
```

Show the list of new commits to the user for confirmation.

---

### Step 7: Push Backport Branch

```bash
git push -u origin backport/<target-sanitized>/pr-<pr-number>
```

If push fails:
- Authentication/permission → stop and surface the error.
- Branch already exists on remote → ask: `force-push (--force-with-lease) / rename / abort`.

---

### Step 8: Open Backport PR

Use `gh` CLI directly so the title format matches the project convention requested:

**Title format:**
```
[<TARGET_BRANCH>] Merge pull request <PR_NUMBER> from <ORIGINAL_HEAD>
```

Example: `[release/5.x] Merge pull request 1234 from feature/fix-logging`

**Body template:**
```markdown
Backport of #<PR_NUMBER> to `<TARGET_BRANCH>`.

**Original PR:** <PR_URL>
**Original title:** <PR_TITLE>
**Original base:** `<ORIGINAL_BASE>`
**Original head:** `<ORIGINAL_HEAD>`
**Merge strategy detected:** <CP_MODE>

## Commits cherry-picked
<bulleted list of `<short-sha> <subject>` from `git log --oneline origin/<TARGET_BRANCH>..HEAD`>

## Test plan
- [ ] Verify the backport builds on `<TARGET_BRANCH>`
- [ ] Re-run the original PR's tests against this branch
```

Create the PR:
```bash
gh pr create \
  --base "<TARGET_BRANCH>" \
  --head "backport/<target-sanitized>/pr-<PR_NUMBER>" \
  --title "[<TARGET_BRANCH>] Merge pull request <PR_NUMBER> from <ORIGINAL_HEAD>" \
  --body "<body content built above>"
```

Pass the body via a heredoc when invoking from the Shell tool:
```bash
gh pr create --base "<TARGET_BRANCH>" \
  --head "backport/<target-sanitized>/pr-<PR_NUMBER>" \
  --title "[<TARGET_BRANCH>] Merge pull request <PR_NUMBER> from <ORIGINAL_HEAD>" \
  --body "$(cat <<'EOF'
Backport of #<PR_NUMBER> to `<TARGET_BRANCH>`.
...
EOF
)"
```

Capture the returned PR URL.

> **Alternative:** The user may instead invoke `/sdlc-workflow:open-pr` after Step 7
> to use the standard PR-opening skill. That path produces a `[KEY] <description>`
> title (derived from the Jira ticket key in the branch name) rather than the
> backport-specific title above. Only use that alternative if the project PR-title
> convention is explicitly preferred for backports.

---

### Step 9: Final Summary

Present a compact summary to the user:

```
Backport Complete

  Original PR:     #<PR_NUMBER> — <PR_TITLE>
                   <PR_URL>
  Target branch:   <TARGET_BRANCH>
  Backport branch: backport/<target-sanitized>/pr-<PR_NUMBER>
  Strategy:        <CP_MODE>
  Commits applied: <count>

Backport PR:       <new PR URL>

Next steps:
  1. Wait for CI to pass on the backport PR.
  2. Request review from the original PR's author/owners.
  3. Merge using the project's standard flow for release branches.
```

---

## Error Handling

### `gh pr view` returns 404
The PR number does not exist or is in a different repo. Ask the user to verify the PR number.

### `mergeCommit.oid` is null but PR is MERGED
Likely a rebase-merged PR. Use `CP_MODE=rebase` and cherry-pick the individual commits.

### Cherry-pick reports "The previous cherry-pick is now empty"
The change is already present on the target branch. Offer:
```
1. Skip this commit (git cherry-pick --skip)
2. Keep as empty commit (git commit --allow-empty)
3. Abort
```

### "refusing to merge unrelated histories"
The target branch is too far diverged. Abort the cherry-pick, report to the user, and
suggest manual backport.

### Push rejected (non-fast-forward on backport branch)
Only occurs if the branch existed remotely. Ask the user to choose:
- `--force-with-lease` (safe force push)
- Rename the local branch and retry
- Abort

---

## Important Notes

- **Never commit directly to the target branch.** All work happens on the dedicated
  `backport/...` branch; the PR is what updates the target branch.
- **Never use plain `git push --force`.** Use `--force-with-lease` if force is needed.
- The skill never merges the created PR — reviewers own that step.
- Keep the original PR's commit authorship intact; cherry-pick preserves `Author:` by default.
  Do not pass `--no-commit` or rewrite authors unless the user explicitly asks.

---

## Files Modified During Execution

- Local git state only: a new branch `backport/<target-sanitized>/pr-<pr-number>`
  pushed to `origin`.
- No files in the working tree are modified outside of cherry-pick content from the
  original PR.

---

## Related

- `/sdlc-workflow:open-pr` — alternative generic PR-opening skill (different title format)