#!/bin/bash
#
# release-rc.sh - cuts an RC2+ pre-release tag.
#
# Owned by external release tooling per PR #10571 / RUN-4938 (setversion.sh explicitly
# refuses rc2+ and points here). Takes the same <version> + tag-type inputs as
# setversion.sh --tag; the calling Rundeck job is expected to route to this script only
# when the tag type is rc2+ (rc1/alpha#/GA/RBA stay on setversion.sh) - this script
# still validates that itself rather than trusting the caller's routing.
#
# What it does:
#   1. Resolves the previous RC tag (new rcN - 1) for the same version and checks it
#      out detached - that commit is the base, not whatever is on the release branch now.
#   2. Attempts to cherry-pick every merged PR labeled $RC_BACKPORT_LABEL, in merge
#      order, regardless of whether it was merge-commit, squash, or rebase merged:
#      merge-commit PRs replay mergeCommit's diff against its mainline parent;
#      squash (and single-commit) PRs cherry-pick mergeCommit alone; multi-commit
#      rebase-merged PRs - where mergeCommit is only the LAST of several commits
#      individually replayed onto the base, detected by comparing its patch-id
#      against the full PR diff's - cherry-pick the whole commit range instead of
#      just the tip, so no earlier commit in the PR is silently dropped. Every PR
#      is attempted - a conflict on one does not stop the others from being tried -
#      and a full status report (applied / already-applied / CONFLICT) is printed
#      at the end.
#   3. Refuses to tag if even one PR failed to apply: a release tag must represent
#      the complete labeled set, never a partial one. Only tags + optionally pushes
#      via create_and_push_tag() (release-tag.sh) when every PR applied cleanly.
#   4. Refuses to tag if NO PR is labeled at all: an rc2+ with nothing to backport
#      almost always means the wrong version was labeled, not a genuine no-op -
#      a deliberate no-op re-tag has to go through release-tag.sh directly instead.
#
# If a rescue/<tag> branch already exists for the tag this invocation would
# create (see the CONFLICT output below - it's how a previous run leaves off
# after a manual resolution), that alone is taken as proof this run should
# finish it rather than start over: it skips resolving/checking out the
# previous RC tag and cherry-picking altogether, and instead verifies every
# currently-labeled PR is already present at the branch's tip (via
# $RC_BACKPORT_APPLIED_LABEL or its cherry-pick trailer - the exact same
# signals used elsewhere in this script) before tagging it directly. Still
# refuses on any PR it can't verify, same as the normal flow refuses on any
# CONFLICT - so re-running after a resolution goes through this same script,
# and its Slack/audit trail, instead of a bare release-tag.sh call that
# bypasses the completeness check entirely.
#
# Usage:
#   release-rc.sh <version> <rc#> [--push] [--dry-run] [--debug]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RELEASE_TAG_SH="$SCRIPT_DIR/release-tag.sh"
RELEASE_VERSION_SH="$SCRIPT_DIR/release-version.sh"

function usage {
    echo "Usage:"
    echo "  release-rc.sh <version> <rc#> [--push] [--dry-run] [--debug]"
    echo ""
    echo "  <version>  MAJOR.MINOR.PATCH, e.g. 7.4.10"
    echo "  <rc#>      rc2 or higher, e.g. rc2, rc3"
    echo "             rc1/alpha#/GA/RBA are not handled here - use setversion.sh for those."
    echo "             If rescue/<tag> already exists for this <version>/<rc#>, it's finished"
    echo "             (verified, then tagged) instead of starting the backport over."
    echo ""
    echo "Flags:"
    echo "  --push      Push the new tag to origin"
    echo "  --dry-run   Show what would be done without making changes"
    echo "  --debug,-v  Enable verbose debug output"
    exit 2
}

if [ -z "${1:-}" ] || [ -z "${2:-}" ]; then
    usage
fi

PUSH_TO_ORIGIN=false
DRY_RUN=false
DEBUG=false

ARGS=()
for arg in "$@"; do
    case "$arg" in
        --push)
            PUSH_TO_ORIGIN=true
            ;;
        --dry-run)
            DRY_RUN=true
            echo "[DRY-RUN MODE] No changes will be made"
            ;;
        --debug|-v)
            DEBUG=true
            set -x
            ;;
        *)
            ARGS+=("$arg")
            ;;
    esac
done
set -- "${ARGS[@]}"

VNUM="${1:-}"
VTAG="${2:-}"
if [ -z "$VNUM" ] || [ -z "$VTAG" ]; then
    usage
fi
if [ "$#" -gt 2 ]; then
    echo "Error: Unexpected argument(s): ${*:3}"
    echo "(a mistyped flag, e.g. --pus instead of --push, lands here silently otherwise)"
    usage
fi

if [ ! -f "$RELEASE_TAG_SH" ]; then
    echo "Error: $RELEASE_TAG_SH not found."
    echo "release-rc.sh depends on create_and_push_tag() from release-tag.sh (see PR #10571 / RUN-4938)."
    exit 6
fi
# shellcheck source=./release-tag.sh
source "$RELEASE_TAG_SH"  # provides create_and_push_tag() and a dry-run-aware git() wrapper
# shellcheck source=./release-version.sh
source "$RELEASE_VERSION_SH"  # provides validate_version_format, parse_rc_number, version_tag_name

# Extend the read/write command lists the git() wrapper (sourced from release-tag.sh)
# checks, with the extra commands this script needs. No need to redefine git()
# itself - it re-reads these lists on every call.
#
# `fetch` is deliberately treated as read-only here even under --dry-run, even
# though it technically updates local remote-tracking refs/tags: a dry run's
# whole point is previewing whether $PREV_TAG/$NEW_TAG already exist against
# real, current remote tag state, which requires actually fetching it - the
# "no changes" promise is about the release-affecting writes below (tagging,
# pushing, cherry-picking), not this incidental, idempotent, easily-repeated
# local ref sync.
RELEASE_GIT_READONLY_CMDS+=" cat-file rev-list fetch"
RELEASE_GIT_WRITE_CMDS+=" cherry-pick"

validate_version_format "$VNUM" || exit 3

# Only rc2+ is handled here - reject rc1/alpha#/GA/RBA even if the caller's routing
# logic misfires, rather than trusting it blindly.
if ! NEW_RC_NUM="$(parse_rc_number "$VTAG")"; then
    echo "Error: '$VTAG' is not a valid RC tag type for release-rc.sh. Expected 'rc<N>'."
    echo "GA/alpha#/RBA/rc1 are handled by setversion.sh, not release-rc.sh."
    exit 3
fi

if [ "$NEW_RC_NUM" -lt 2 ]; then
    echo "Error: release-rc.sh only handles rc2 and above. Use setversion.sh for rc1."
    exit 3
fi

VERSION="$VNUM"
NEW_TAG="$(version_tag_name "$VERSION" "$VTAG")"
PREV_RC_NUM=$((NEW_RC_NUM - 1))
PREV_TAG="$(version_tag_name "$VERSION" "rc$PREV_RC_NUM")"

# Scoped to this exact version, not a single global label - so two release lines
# in flight at once (e.g. cutting 6.2.0-rc2 while 6.1.1 is being prepared, or even
# 6.2.0-rc2 while 6.2.1-rc1 is being prepared) can't have a PR meant for one
# version swept into another's RC by sharing one generic label. Matches the
# granularity NEW_TAG/PREV_TAG already use (full MAJOR.MINOR.PATCH) - major.minor
# alone would still collide between patch releases of the same minor.
RC_BACKPORT_LABEL="rc-backport-$VERSION"
readonly RC_BACKPORT_LABEL

# Applied (by this script, or by a human resolving a CONFLICT manually per the
# rescue-branch instructions below) to a PR once it has actually landed in this
# version's RC lineage. This is the authoritative "already applied" signal - the
# cherry-pick commit trailer this script stamps via `-x` (see ALREADY_APPLIED
# below) only survives when the exact same automated flow is used to continue a
# cherry-pick; a human resolving a conflict is free to write any commit message,
# so a manual backport that doesn't preserve the trailer would otherwise look
# unapplied to the next RC run and get (harmlessly, but confusingly) re-attempted.
#
# Deliberately NOT named "backport-completed" - both rundeck and rundeckpro
# already have a generic label with that exact name, paired with the unrelated
# auto-backport/backport-to-release/X.Y.x GitHub Action that backports merged
# PRs to maintenance branches post-GA (see .github/workflows/backport.yml).
# Reusing that name here, even with a version suffix, would read as belonging
# to that system. This is scoped per-version like $RC_BACKPORT_LABEL, and kept
# in the same "rc-backport-" family so `gh pr list --label` filters can never
# ambiguously match either the unrelated label or $RC_BACKPORT_LABEL itself.
RC_BACKPORT_APPLIED_LABEL="rc-backport-$VERSION-applied"
readonly RC_BACKPORT_APPLIED_LABEL

echo "New RC tag:      $NEW_TAG"
echo "Previous RC tag: $PREV_TAG"
echo "PR label:        $RC_BACKPORT_LABEL"
echo "Applied label:   $RC_BACKPORT_APPLIED_LABEL"

if [ "$DRY_RUN" = true ]; then
    echo "[DRY-RUN] gh label create $RC_BACKPORT_APPLIED_LABEL --force"
else
    gh label create "$RC_BACKPORT_APPLIED_LABEL" \
        --description "Applied (by release-rc.sh or manually) to the $VERSION RC lineage - unrelated to the generic 'backport-completed' label" \
        --color BFD4F2 --force >/dev/null
fi

git fetch --tags --quiet

if git rev-parse --verify "refs/tags/$NEW_TAG^{commit}" >/dev/null 2>&1; then
    echo "Error: tag '$NEW_TAG' already exists."
    exit 5
fi

# A rescue/<tag> branch already existing for the tag this run would create is
# proof enough that a previous run left off here (see the CONFLICT output
# below) - resume from it instead of starting the backport over. Checked on
# both origin (the normal case - a previous --push run) and locally (in case
# --push wasn't used, or this happens to be the same checkout).
RESCUE_BRANCH="rescue/$NEW_TAG"
RESUME_COMMIT=""
if git ls-remote --exit-code --heads origin "$RESCUE_BRANCH" >/dev/null 2>&1; then
    git fetch --quiet origin "$RESCUE_BRANCH"
    RESUME_COMMIT="$(git rev-parse FETCH_HEAD)"
elif git rev-parse --verify "refs/heads/$RESCUE_BRANCH" >/dev/null 2>&1; then
    RESUME_COMMIT="$(git rev-parse "refs/heads/$RESCUE_BRANCH")"
fi

if [ -n "$RESUME_COMMIT" ]; then
    echo "Found existing $RESCUE_BRANCH ($RESUME_COMMIT) - resuming from it instead of starting the backport over."
    TRAILER_SCAN_BASE="$RESUME_COMMIT"
else
    if ! git rev-parse --verify "refs/tags/$PREV_TAG^{commit}" >/dev/null 2>&1; then
        echo "Error: previous RC tag '$PREV_TAG' not found. Cannot determine base commit."
        exit 4
    fi
    PREV_COMMIT="$(git rev-parse "refs/tags/$PREV_TAG^{commit}")"
    echo "Checking out $PREV_TAG ($PREV_COMMIT) detached"
    git checkout --detach "$PREV_COMMIT"
    TRAILER_SCAN_BASE="$PREV_COMMIT"
fi

echo "Looking up merged PRs labeled '$RC_BACKPORT_LABEL'..."
# A fixed page size could silently truncate the result, after which the checks
# below would treat the truncated list as the complete labeled set and tag an
# incomplete RC while reporting success. PR_LABEL_FETCH_LIMIT is set far above
# any realistic number of PRs backported into a single RC line, but the count
# is still checked against it below rather than trusted blindly.
PR_LABEL_FETCH_LIMIT=1000
PR_RAW_COUNT="$(gh pr list --label "$RC_BACKPORT_LABEL" --state merged -L "$PR_LABEL_FETCH_LIMIT" \
    --json number --jq 'length')"
if [ "$PR_RAW_COUNT" -ge "$PR_LABEL_FETCH_LIMIT" ]; then
    echo "Error: gh pr list returned $PR_RAW_COUNT PRs labeled '$RC_BACKPORT_LABEL', at or above the fetch"
    echo "limit of $PR_LABEL_FETCH_LIMIT - there may be more matching merged PRs than were fetched, which would let"
    echo "a partial set be tagged as complete. Raise PR_LABEL_FETCH_LIMIT in this script and re-run."
    exit 9
fi
PR_DATA="$(gh pr list --label "$RC_BACKPORT_LABEL" --state merged -L "$PR_LABEL_FETCH_LIMIT" \
    --json number,title,mergedAt,mergeCommit,labels,commits \
    --jq 'sort_by(.mergedAt)[] | select(.mergeCommit != null) | [.number, .mergeCommit.oid, .title, ([.labels[].name] | join(",")), (.commits | length)] | @tsv')"

# Every automated cherry-pick below uses `-x`, which appends "(cherry picked from
# commit <sha>)" to the resulting commit message. That trailer is what makes
# "already applied" trackable across RC generations without the label below: rc3
# is cherry-picked on top of rc2, which already carries rc1's and rc2's trailers in
# its history, so grepping $PREV_COMMIT's ancestry for a PR's original merge-commit
# SHA reliably tells us it's already in the lineage - comparing the PR's original
# SHA directly against our history (via merge-base --is-ancestor) does NOT work,
# since cherry-pick always creates a brand new SHA.
#
# This is a secondary signal, though: $RC_BACKPORT_APPLIED_LABEL (checked per-PR
# below) is authoritative and catches manual backports that don't preserve the
# trailer. Both are checked so a PR is never re-attempted just because one of the
# two signals is missing.
#
# `|| true` guards the ordinary "no trailers yet" case (e.g. cutting the very
# first rc2 of a release line, where $TRAILER_SCAN_BASE is rc1 and carries none) -
# under `set -euo pipefail` an empty grep match exits 1, and since this is a
# plain assignment (not part of an if/while condition), that would otherwise
# kill the script here, before a single PR is even looked up.
#
# $TRAILER_SCAN_BASE is $PREV_COMMIT normally, or $RESUME_COMMIT when finishing
# a manually-resolved rescue branch - either way, it's the commit whose ancestry
# is checked for each PR's trailer.
ALREADY_APPLIED="$(git log --format=%B "$TRAILER_SCAN_BASE" | grep -oE '\(cherry picked from commit [0-9a-f]{40}\)' | grep -oE '[0-9a-f]{40}' || true)"

# Per-PR outcome, tab-separated: number / sha / status / detail / title.
# status is one of: already-applied | applied | CONFLICT
REPORT=()
CONFLICT_COUNT=0
# PR numbers cherry-picked fresh in this run - only labeled $RC_BACKPORT_APPLIED_LABEL
# once $NEW_TAG is actually created (see below), never earlier: labeling them as
# soon as each cherry-pick succeeds would be premature - if a LATER PR in this
# same run conflicts, the run aborts without tagging (only a rescue branch is
# saved), but those earlier PRs would already carry the label. A rerun then
# starts fresh from $PREV_TAG, sees the label, and skips them even though their
# commits only ever existed on the abandoned/rescue candidate - producing an RC
# that's missing them while still reporting success.
NEWLY_APPLIED_PR_NUMS=()
# PR number -> the exact command that would apply it, for the resolve
# instructions below - single-commit vs range (rebase-merge) needs a different
# command, so a single generic hint would be wrong for some CONFLICT PRs.
declare -A RESUME_CMDS

if [ -z "$PR_DATA" ]; then
    echo "Error: No merged PRs found with label '$RC_BACKPORT_LABEL'. Refusing to cut $NEW_TAG with nothing to backport -"
    echo "this almost always means the wrong version was labeled, or nothing was labeled yet, not that $NEW_TAG is"
    echo "genuinely a no-op re-tag of $TRAILER_SCAN_BASE."
    echo "If a no-op re-tag is really what's intended, do it explicitly instead of through this script:"
    echo "  $RELEASE_TAG_SH $NEW_TAG $TRAILER_SCAN_BASE [--push]"
    exit 8
else
    mapfile -t PR_LINES <<< "$PR_DATA"
    for line in "${PR_LINES[@]}"; do
        IFS=$'\t' read -r PR_NUM PR_SHA PR_TITLE PR_LABELS_CSV PR_COMMIT_COUNT <<< "$line"

        ALREADY_LABELED=false
        if [[ ",$PR_LABELS_CSV," == *",$RC_BACKPORT_APPLIED_LABEL,"* ]]; then
            ALREADY_LABELED=true
        fi

        if [ "$ALREADY_LABELED" = true ] || grep -qxF "$PR_SHA" <<< "$ALREADY_APPLIED"; then
            if [ "$ALREADY_LABELED" = true ]; then
                echo "PR #$PR_NUM ($PR_SHA): already labeled '$RC_BACKPORT_APPLIED_LABEL', skipping"
            else
                echo "PR #$PR_NUM ($PR_SHA): already cherry-picked into a previous RC, skipping"
            fi
            REPORT+=("$PR_NUM"$'\t'"$PR_SHA"$'\t'"already-applied"$'\t'"-"$'\t'"$PR_TITLE")
            # Backfill the label if only the trailer caught it (e.g. a PR applied by an
            # earlier run of this script, before this label existed) - keeps every
            # completed PR converging on the same signal going forward.
            if [ "$ALREADY_LABELED" = false ]; then
                if [ "$DRY_RUN" = true ]; then
                    echo "[DRY-RUN] gh pr edit $PR_NUM --add-label $RC_BACKPORT_APPLIED_LABEL"
                else
                    gh pr edit "$PR_NUM" --add-label "$RC_BACKPORT_APPLIED_LABEL" \
                        || echo "  Warning: failed to backfill '$RC_BACKPORT_APPLIED_LABEL' on PR #$PR_NUM"
                fi
            fi
            continue
        fi

        if [ -n "$RESUME_COMMIT" ]; then
            # Resuming only ever verifies - it never cherry-picks onto $RESUME_COMMIT,
            # since that would mean guessing at a base HEAD isn't even checked out
            # to. A PR that isn't already-applied here is a genuine gap in the
            # rescue branch, not something to fix by picking onto an unrelated tree.
            echo "PR #$PR_NUM ($PR_SHA): NOT found at $RESUME_COMMIT (no '$RC_BACKPORT_APPLIED_LABEL' label, no matching cherry-pick trailer)"
            REPORT+=("$PR_NUM"$'\t'"$PR_SHA"$'\t'"CONFLICT"$'\t'"missing from $RESCUE_BRANCH"$'\t'"$PR_TITLE")
            CONFLICT_COUNT=$((CONFLICT_COUNT + 1))
            continue
        fi

        echo "Cherry-picking PR #$PR_NUM ($PR_SHA): $PR_TITLE"

        # A merge-commit-strategy PR has multiple parents; replay only the diff
        # against its first (mainline) parent - that one commit's diff against
        # mainline is always the PR's whole diff, regardless of how many commits
        # the PR had, so a single cherry-pick is always correct here.
        PARENTS=($(git log -1 --pretty=%P "$PR_SHA"))
        CHERRY_PICK_TARGET="$PR_SHA"
        CHERRY_PICK_ARGS=(-x)
        if [ "${#PARENTS[@]}" -gt 1 ]; then
            CHERRY_PICK_ARGS+=(-m 1)
        elif [ "${PR_COMMIT_COUNT:-1}" -gt 1 ]; then
            # Single-parent PRs with more than one original commit are ambiguous:
            # squash-merge folds every commit into this one (single cherry-pick is
            # the whole PR diff, correct), but rebase-merge replays each original
            # commit individually onto the base as its own commit and mergeCommit
            # is only the LAST of that chain - cherry-picking just the tip would
            # silently drop every earlier commit in the PR. (Verified against a
            # real rebase-merged PR: a 4-commit PR's mergeCommit alone touched only
            # 1 of the 4 changed files.) Parent count can't tell these apart -
            # squash also produces a single-parent commit - so compare the tip
            # commit's patch-id against the full PR's patch-id: identical means
            # squash (or a rebase of a PR whose commits happen to net the same
            # diff as its last commit alone, vanishingly unlikely in practice);
            # different means rebase-merge, and the range of the last
            # PR_COMMIT_COUNT first-parent commits ending at mergeCommit is
            # cherry-picked instead of just the tip.
            FULL_PR_PATCH_ID="$(gh pr diff "$PR_NUM" | git patch-id --stable | awk '{print $1}')"
            TIP_PATCH_ID="$(git diff "$PR_SHA"^ "$PR_SHA" | git patch-id --stable | awk '{print $1}')"
            if [ -n "$FULL_PR_PATCH_ID" ] && [ "$FULL_PR_PATCH_ID" != "$TIP_PATCH_ID" ]; then
                RANGE_BASE="$PR_SHA~$PR_COMMIT_COUNT"
                if git rev-parse --verify "${RANGE_BASE}^{commit}" >/dev/null 2>&1; then
                    echo "  Detected rebase-merge of $PR_COMMIT_COUNT commits (mergeCommit alone doesn't match the full PR diff) - cherry-picking the whole range $RANGE_BASE..$PR_SHA instead of just the tip"
                    CHERRY_PICK_TARGET="${RANGE_BASE}..${PR_SHA}"
                else
                    # Refusing to fall back to a single-commit pick here - that would
                    # silently ship an INCOMPLETE backport, the exact failure mode this
                    # whole range-detection exists to prevent. Treated as a CONFLICT so
                    # it blocks the tag like any other unresolved PR, instead of a
                    # warning that's easy to miss in a long run's output.
                    echo "  Detected rebase-merge of $PR_COMMIT_COUNT commits, but $RANGE_BASE isn't reachable (shallow history?) - refusing to cherry-pick only the tip. Marking as CONFLICT."
                    REPORT+=("$PR_NUM"$'\t'"$PR_SHA"$'\t'"CONFLICT"$'\t'"range base $RANGE_BASE unreachable - fetch full history"$'\t'"$PR_TITLE")
                    RESUME_CMDS["$PR_NUM"]="git fetch --unshallow (or otherwise deepen history) then: git cherry-pick -x ${RANGE_BASE}..${PR_SHA}"
                    CONFLICT_COUNT=$((CONFLICT_COUNT + 1))
                    continue
                fi
            fi
        fi

        RESUME_CMDS["$PR_NUM"]="git cherry-pick ${CHERRY_PICK_ARGS[*]} $CHERRY_PICK_TARGET"

        if git cherry-pick "${CHERRY_PICK_ARGS[@]}" "$CHERRY_PICK_TARGET"; then
            REPORT+=("$PR_NUM"$'\t'"$PR_SHA"$'\t'"applied"$'\t'"$(git rev-parse --short HEAD)"$'\t'"$PR_TITLE")
            NEWLY_APPLIED_PR_NUMS+=("$PR_NUM")
        else
            # Deliberately not auto-resolving (e.g. -X ours/theirs) - guessing wrong on a
            # release cherry-pick ships a silent regression, which is worse than stopping here.
            # Abort just this PR and keep going, so one conflict doesn't hide the status of
            # every other labeled PR - the full picture is what lets a human triage in one pass.
            CONFLICT_FILES="$(git diff --name-only --diff-filter=U | paste -sd ',' -)"
            git cherry-pick --abort
            echo "  conflict on: $CONFLICT_FILES - left out, continuing with the rest"
            REPORT+=("$PR_NUM"$'\t'"$PR_SHA"$'\t'"CONFLICT"$'\t'"$CONFLICT_FILES"$'\t'"$PR_TITLE")
            CONFLICT_COUNT=$((CONFLICT_COUNT + 1))
        fi
    done
fi

echo ""
echo "=== Cherry-pick report ($PREV_TAG -> candidate for $NEW_TAG) ==="
printf '%-6s %-10s %-16s %-30s %s\n' "PR" "SHA" "STATUS" "DETAIL" "TITLE"
for entry in "${REPORT[@]}"; do
    IFS=$'\t' read -r R_NUM R_SHA R_STATUS R_DETAIL R_TITLE <<< "$entry"
    printf '%-6s %-10s %-16s %-30s %s\n' "#$R_NUM" "$R_SHA" "$R_STATUS" "$R_DETAIL" "$R_TITLE"
done
echo ""

if [ -n "$RESUME_COMMIT" ]; then
    FINAL_COMMIT="$RESUME_COMMIT"
else
    FINAL_COMMIT="$(git rev-parse HEAD)"
fi

if [ "$CONFLICT_COUNT" -gt 0 ]; then
    if [ -n "$RESUME_COMMIT" ]; then
        # Resuming never cherry-picks (see above), so there's nothing new to save
        # to a rescue branch - $RESCUE_BRANCH is exactly the one just checked and
        # still genuinely missing PR(s). Point back at it instead of restating the
        # full cherry-pick instructions, which don't apply to how it got here.
        echo ""
        echo "Error: $RESCUE_BRANCH ($RESUME_COMMIT) is still missing $CONFLICT_COUNT PR(s) marked CONFLICT above."
        echo "Refusing to tag $NEW_TAG - a release tag must include every PR labeled '$RC_BACKPORT_LABEL', never a partial set."
        echo ""
        echo "To resolve: on $RESCUE_BRANCH, cherry-pick the still-missing PR(s), label each"
        echo "$RC_BACKPORT_APPLIED_LABEL as you go, push, then re-run this same command."
        exit 7
    fi

    if [ "$DRY_RUN" = true ]; then
        echo "[DRY-RUN] git branch -f $RESCUE_BRANCH HEAD"
    else
        command git branch -f "$RESCUE_BRANCH" "$FINAL_COMMIT"
        echo "Every PR marked 'applied' above is saved to local branch '$RESCUE_BRANCH' ($FINAL_COMMIT) - none of that work is lost."
        if [ "$PUSH_TO_ORIGIN" = true ]; then
            command git push --force origin "$RESCUE_BRANCH"
            echo "Also pushed to origin/$RESCUE_BRANCH"
        fi
    fi

    echo ""
    echo "Error: $CONFLICT_COUNT PR(s) marked CONFLICT above could not be cherry-picked cleanly."
    echo "Refusing to tag $NEW_TAG - a release tag must include every PR labeled '$RC_BACKPORT_LABEL', never a partial set."
    echo ""
    echo "To resolve:"
    echo "  1. git checkout $RESCUE_BRANCH"
    echo "  2. For each CONFLICT PR above, run its specific command (single-commit vs. range differs"
    echo "     per PR - using the wrong one can drop commits), resolve the listed files, git add <files>,"
    echo "     git cherry-pick --continue:"
    for entry in "${REPORT[@]}"; do
        IFS=$'\t' read -r R_NUM R_SHA R_STATUS R_DETAIL R_TITLE <<< "$entry"
        if [ "$R_STATUS" = "CONFLICT" ]; then
            echo "       PR #$R_NUM: ${RESUME_CMDS[$R_NUM]:-git cherry-pick -x $R_SHA}"
        fi
    done
    echo "     Then label it, regardless of the commit message used to continue -"
    echo "     future RC runs rely on this label, not the commit message, to know it's done:"
    echo "       gh pr edit <PR#> --add-label $RC_BACKPORT_APPLIED_LABEL"
    echo "  3. Once every PR is in, push $RESCUE_BRANCH (if not already) and re-run this same command -"
    echo "     it will find the branch and finish by tagging it, instead of starting over:"
    echo "       git push origin $RESCUE_BRANCH"
    exit 7
fi

echo "All labeled PRs verified present. Final commit for $NEW_TAG: $FINAL_COMMIT"
if create_and_push_tag "$NEW_TAG" "$FINAL_COMMIT" "Release $VERSION rc$NEW_RC_NUM"; then
    # Only now that $NEW_TAG genuinely exists are freshly-cherry-picked PRs
    # labeled - see NEWLY_APPLIED_PR_NUMS above for why not sooner.
    if [ "${#NEWLY_APPLIED_PR_NUMS[@]}" -gt 0 ]; then
        echo ""
        echo "Labeling newly-applied PRs '$RC_BACKPORT_APPLIED_LABEL' now that $NEW_TAG exists..."
        for PR_NUM in "${NEWLY_APPLIED_PR_NUMS[@]}"; do
            if [ "$DRY_RUN" = true ]; then
                echo "[DRY-RUN] gh pr edit $PR_NUM --add-label $RC_BACKPORT_APPLIED_LABEL"
            else
                gh pr edit "$PR_NUM" --add-label "$RC_BACKPORT_APPLIED_LABEL" \
                    || echo "  Warning: failed to add '$RC_BACKPORT_APPLIED_LABEL' to PR #$PR_NUM - label it manually so future RC runs recognize it's applied"
            fi
        done
    fi
else
    exit $?
fi
