# Release tagging: end-to-end scenarios

Reference for `setversion.sh`, `release-rc.sh`, `release-tag.sh`, and `release-version.sh`, and for the "Release Core/Pro (Tag-based, Auto-Backport)" Rundeck jobs that wrap them. Covers every scenario the tooling is designed to handle, what happens, and why.

## Routing: which script runs

| `releaseType` | Script | Commit input |
|---|---|---|
| `alpha1`, `alpha2`, `rc1` | `setversion.sh --tag <version> <type> <commit>` | **Required** - no release branch exists yet to resolve one from |
| `GA` | `setversion.sh --tag <version> GA` | **Forbidden** - re-tags the highest existing RC for the version |
| `rc2`, `rc3`, ... | `release-rc.sh <version> <rc#>` | **Forbidden** - resolved automatically (previous RC tag, or an existing `rescue/<tag>` branch) |

The Rundeck job fails fast on a mismatched `releaseType`/commit combination, before any git operation runs.

## The label system

- **`rc-backport-<version>`** (e.g. `rc-backport-6.2.0`) - applied by a human to a merged PR to mark it for backport into that version's RC line. Scoped to the full version (not just major.minor, and not tied to a specific rc number) - one label covers every RC of that version, from rc2 through however many are cut.
- **`rc-backport-<version>-applied`** - applied by the script once a PR is confirmed part of a tagged (or about-to-be-tagged) RC. Authoritative "already applied" signal, checked before the cherry-pick trailer. **If you ever manually cherry-pick a labeled PR into the RC lineage yourself, add this label immediately** - the next run doesn't know a PR is in unless the label or trailer says so, and will otherwise try to re-apply it.
- Not to be confused with the pre-existing, unrelated **`backport-completed`** label (paired with `auto-backport`/`backport-to-release/X.Y.x`), which belongs to a different system entirely: backporting merged PRs to maintenance branches post-GA.

## Scenario 1: Normal rc2+ cut, everything applies cleanly

```
release-rc.sh 6.2.0 rc2 --push
```

1. Resolves `v6.2.0-rc1` (the previous RC tag) and checks it out detached.
2. Looks up every merged PR labeled `rc-backport-6.2.0`.
3. Cherry-picks each one not already applied, in merge order, onto that base.
4. Tags the result `v6.2.0-rc2` and pushes it.
5. Labels every freshly-applied PR `rc-backport-6.2.0-applied` - **only now**, after the tag exists (see Scenario 3 for why not sooner).

## Scenario 2: Nothing labeled for this version

```
release-rc.sh 6.2.0 rc2 --push
# Error: No merged PRs found with label 'rc-backport-6.2.0'. Refusing to cut v6.2.0-rc2 ...
```

Refuses outright (exit 8) rather than silently re-tagging the previous RC's commit as-is. An rc2+ run with nothing labeled almost always means the wrong version was labeled, or nothing was labeled yet - not a genuine no-op release. A deliberate no-op re-tag has to go through `release-tag.sh` directly (the script prints the exact command).

## Scenario 3: A PR conflicts - CONFLICT and the rescue branch

```
release-rc.sh 6.2.0 rc3 --push
# ... PR #1234 conflict on: some/file.groovy - left out, continuing with the rest
# Error: 1 PR(s) marked CONFLICT above could not be cherry-picked cleanly.
```

- Every *other* labeled PR still gets attempted - one conflict doesn't hide the status of the rest.
- Everything that **did** apply cleanly is saved to a local `rescue/v6.2.0-rc3` branch (pushed too, with `--push`) - none of that work is lost.
- **Nothing is labeled yet**, even for the PRs that successfully cherry-picked in this run. Labeling happens only after the tag actually exists - labeling early and then aborting without tagging would mean a later, fresh run sees the label, skips that PR, and produces an RC silently missing it while still reporting success.
- The script refuses to tag (exit 7) and prints, **for each CONFLICT PR specifically**, the exact `git cherry-pick` command to resolve it by hand (a plain SHA for most PRs, or a `<range-base>..<tip>` range for a multi-commit rebase-merge - see Scenario 5, using the wrong form silently drops commits).

## Scenario 4: Finishing a conflict - the rescue branch resume

After manually resolving on `rescue/v6.2.0-rc3` (cherry-picking the remaining PR(s), pushing the branch), **re-run the exact same command**:

```
release-rc.sh 6.2.0 rc3 --push
# Found existing rescue/v6.2.0-rc3 (abc1234) - resuming from it instead of starting the backport over.
```

- The script detects `rescue/v6.2.0-rc3` already exists (checked on origin first, then locally) and treats that alone as proof this run should finish it, not start the backport over.
- It does **not** cherry-pick anything in this mode. Instead it verifies every currently-labeled PR is present at the branch's tip, via the `-applied` label or the cherry-pick trailer.
- If every PR checks out, it tags the branch's tip directly and backfills the `-applied` label on any PR that only had the trailer.
- If anything is still missing, it refuses again (exit 7) and points back at the same branch.

This keeps the whole flow - and its Slack/audit trail - inside the Rundeck job, instead of requiring a bare `release-tag.sh` call that bypasses the completeness check entirely.

**Label the PR immediately after any manual cherry-pick**, on the rescue branch or otherwise - the resume verification (and every future run) relies on the label or trailer, not on you remembering it got in.

## Scenario 5: A rebase-merged, multi-commit PR

GitHub's "Rebase and merge" replays each of a PR's original commits individually onto the base - `mergeCommit` is only the **last** of that chain, not a single commit representing the whole PR (unlike a squash merge, which really is one commit for the whole diff). Cherry-picking just `mergeCommit` for such a PR would silently drop every earlier commit.

The script detects this by comparing `mergeCommit`'s patch-id against the full PR diff's patch-id:
- **Match** -> squash-merge (or a naturally single-commit PR) - single cherry-pick, as normal.
- **Mismatch** -> rebase-merge - cherry-picks the whole range (`mergeCommit~N..mergeCommit`, where N = the PR's original commit count) in one shot instead of just the tip.
- If the range's base commit isn't reachable (e.g. shallow history), it's marked **CONFLICT** rather than silently falling back to an incomplete single-commit pick.

## Scenario 6: Multiple RC generations, some PRs old, some new

Say 3 PRs were backported into `rc2`, and 2 more get labeled and merged before `rc3` is cut:

```
release-rc.sh 6.2.0 rc3 --push
```

- Looks up **every** PR ever labeled `rc-backport-6.2.0` - all 5, regardless of which rc they were originally meant for (the label doesn't encode an rc number).
- The 3 from `rc2` are found already-applied (via label or trailer, since `rc2`'s tag is now `rc3`'s base) and skipped.
- The 2 new ones get cherry-picked fresh.
- Result: `rc3` = `rc2` + the 2 new PRs, correctly, with no re-application and no omissions.

## Scenario 7: `alpha#`/`rc1` (needs a commit) and `GA` (must not have one)

```
setversion.sh --tag 6.2.0 rc1 <commit> --push   # commit required
setversion.sh --tag 6.2.0 GA --push             # no commit - re-tags the highest existing RC
```

The Rundeck job's `Commit SHA` option enforces this before running anything:
- Given for `GA` or `rc2`+ -> job fails fast, error names which type forbids it.
- Missing for `rc1`/`alpha1`/`alpha2` -> job fails fast, error names which type requires it.
- Accepts either a bare SHA or a full GitHub commit URL (the URL prefix is stripped automatically).

## Reference: exit codes (`release-rc.sh`)

| Code | Meaning |
|---|---|
| 2 | Usage error (missing args, unexpected extra argument, unknown `<rc#>`) |
| 3 | Bad `<version>` format, or `<rc#>` isn't `rc2` or higher |
| 4 | Previous RC tag not found (and not resuming from a rescue branch) |
| 5 | `<version>-<rc#>` tag already exists |
| 6 | `release-tag.sh` not found, or (resume mode) the rescue branch's commit doesn't resolve |
| 7 | One or more PRs still CONFLICT/missing - see the rescue branch and per-PR resume commands |
| 8 | No PR is labeled for this version at all |
| 9 | The labeled-PR fetch hit its safety cap - there may be more than were retrieved |
