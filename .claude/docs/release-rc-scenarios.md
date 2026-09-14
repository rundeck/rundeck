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
- **`rc-backport-<version>-applied`** - applied by the script once a PR is confirmed part of a tagged (or about-to-be-tagged) RC. Authoritative "already applied" signal, checked before the cherry-pick trailer. **If you ever manually cherry-pick a labeled PR into the RC lineage yourself, add this label immediately** - the next run doesn't know a PR is in unless the label or trailer says so, and will otherwise try to re-apply it. The script itself only ever adds this label **after `--push` and after the tag genuinely exists** - never speculatively - since a local-only tag can vanish with the checkout while the label would remain, durable and visible to everyone, on GitHub.
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
- A PR whose commit isn't even resolvable in this checkout (shallow/partial clone, pruned object) is marked CONFLICT immediately, without ever attempting a cherry-pick - and a real conflict's cleanup (`git cherry-pick --abort`) itself failing (rare, but possible if the pick failed before starting a resumable sequencer state at all) is caught too. Neither can crash the whole run and lose the report/rescue branch for every PR that already succeeded.

## Scenario 4: Finishing a conflict - the rescue branch resume

After manually resolving on `rescue/v6.2.0-rc3` (cherry-picking the remaining PR(s), pushing the branch), **re-run the exact same command**:

```
release-rc.sh 6.2.0 rc3 --push
# Found existing rescue/v6.2.0-rc3 (abc1234) - resuming from it instead of starting the backport over.
```

- The script detects `rescue/v6.2.0-rc3` already exists (checked on origin first, then locally) and treats that alone as proof this run should finish it, not start the backport over.
- **It doesn't trust the branch blindly**: its tip must be a verified descendant of `v6.2.0-rc2` (the previous RC tag), via `git merge-base --is-ancestor`. A branch that happens to share the name but isn't actually built on the right base - stale, from an unrelated attempt, force-pushed over - is refused (exit 10), never silently tagged.
- It does **not** cherry-pick anything in this mode. Instead it verifies every currently-labeled PR is present at the branch's tip, via the `-applied` label or the cherry-pick trailer.
- If every PR checks out, it tags the branch's tip directly and backfills the `-applied` label on any PR that only had the trailer.
- If anything is still missing, it refuses again (exit 7) and points back at the same branch.

This keeps the whole flow - and its Slack/audit trail - inside the Rundeck job, instead of requiring a bare `release-tag.sh` call that bypasses the completeness check entirely.

**Push before you label.** After resolving on the rescue branch, `git push origin rescue/v6.2.0-rc3` *first*, then label the PR(s) you just resolved - never the other way around. The label is trusted on its own (no ancestry check), so labeling a commit that only exists in your local checkout would let a run started elsewhere skip that PR while its commits don't durably exist anywhere yet.

**The rescue branch is never force-created or force-pushed.** If one already exists locally or on origin with different content than what this run just produced - a race with a concurrent run, or in-progress manual work the auto-detection above somehow missed - creating/pushing refuses (exit 11) rather than overwriting it. Investigate what's there before proceeding.

**Once `v6.2.0-rc3` is genuinely created and pushed, `rescue/v6.2.0-rc3` is deleted** - both locally and on origin (with `--push`) - since the tag is now the durable record and the branch would just be confusing leftover state otherwise. This happens whether this run resumed from the branch or it turned out to be unrelated leftover from something else. In resume mode, each ref's tip is re-checked immediately before deleting it against the exact SHA this run verified and tagged - if something else moved it since (a manual push landing mid-run), it's left in place with a warning instead of being force-deleted.

## Cleanup after a failed tag push

If `git push` for the new tag itself fails (network blip, permissions, etc.) after the tag was already created locally, the local-only tag is deleted immediately rather than left behind - otherwise a retry would fail at tag creation ("already exists") before ever reaching the push again. This applies to both `release-rc.sh` and `setversion.sh` (they share the same `create_and_push_tag` in `release-tag.sh`).

## Dry-run is a real rehearsal, not just a preview

`--dry-run` actually checks out the base commit and attempts every cherry-pick for real (aborting cleanly on conflict) - both are fully local and reversible, so a dry run genuinely tells you whether the backport would succeed, including real `CONFLICT`s, instead of unconditionally reporting every PR as "applied." Only the release-affecting writes stay simulated: creating/pushing the tag, creating/pushing the rescue branch, and applying GitHub labels are all printed as `[DRY-RUN] ...` rather than executed.

## Scenario 5: A rebase-merged, multi-commit PR

GitHub's "Rebase and merge" replays each of a PR's original commits individually onto the base - `mergeCommit` is only the **last** of that chain, not a single commit representing the whole PR (unlike a squash merge, which really is one commit for the whole diff). Cherry-picking just `mergeCommit` for such a PR would silently drop every earlier commit.

The script detects this in two stages, since a tip mismatch alone isn't proof - GitHub's rendered PR diff and a local `git diff` of the same content can differ for reasons unrelated to merge strategy (rename detection, diff config), which could otherwise misclassify a squash PR and let the computed range sweep in unrelated commits from mainline history:
1. Compare `mergeCommit`'s patch-id against the full PR diff's patch-id. **Match** -> squash-merge (or a naturally single-commit PR) - single cherry-pick, as normal, nothing further to check. **Mismatch** -> possibly rebase-merge, continue to step 2.
2. Compute the range `mergeCommit~N..mergeCommit` (N = the PR's original commit count) and verify *its own* aggregate patch-id also matches the full PR diff. **Match** -> confirmed rebase-merge, cherry-pick the whole range instead of just the tip. **No match** (or the range's base commit isn't reachable, e.g. shallow history) -> marked **CONFLICT** for manual classification - this is an expected, documented outcome when the PR's history doesn't cleanly fit either model, not an undocumented failure - rather than guessing and risking an incomplete or incorrect backport.

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

Only the codes the script actually assigns via an explicit `exit N`. A few other failures (e.g. `git rev-parse`/`git merge-base` erroring on something unexpected) propagate whatever raw exit status *that* command returned, under `set -e` - not one of these.

| Code | Meaning |
|---|---|
| 2 | Usage error: missing `<version>`/`<rc#>`, or an unexpected extra positional argument |
| 3 | Bad `<version>` format; `<rc#>` doesn't match `rc<N>`; or `<rc#>` is below `rc2` |
| 4 | Previous RC tag not found |
| 5 | `<version>-<rc#>` tag already exists |
| 6 | `release-tag.sh` not found next to this script |
| 7 | One or more PRs still CONFLICT/missing - see the rescue branch and per-PR resume commands |
| 8 | No PR is labeled for this version at all |
| 9 | The labeled-PR fetch hit its safety cap - there may be more than were retrieved |
| 10 | A `rescue/<tag>` branch exists but isn't a descendant of the previous RC tag - refused rather than trusted |
| 11 | A `rescue/<tag>` branch (or its remote) already exists with different content than this run's candidate - refused rather than force-overwritten |
| 13 | A labeled, merged PR has no resolvable `mergeCommit` - refused rather than silently dropped from the set |
| 14 | The labeled PR set changed (a PR was labeled, unlabeled, or newly merged) since this run started - refused rather than tagging a stale snapshot |
| 15 | Both a local and an `origin` `rescue/<tag>` branch exist with different tips - refused rather than guessing which is authoritative |
