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
#   2. Attempts to cherry-pick the merge commit of every merged PR labeled
#      $RC_BACKPORT_LABEL, in merge order, regardless of whether the PR was
#      merge-commit, squash, or rebase merged. Every PR is attempted - a conflict on
#      one does not stop the others from being tried - and a full status report
#      (applied / already-applied / CONFLICT) is printed at the end.
#   3. Refuses to tag if even one PR failed to apply: a release tag must represent
#      the complete labeled set, never a partial one. Only tags + optionally pushes
#      via create_and_push_tag() (release-tag.sh) when every PR applied cleanly.
#
# Usage:
#   release-rc.sh <version> <rc#> [--push] [--dry-run] [--debug]
#
# Example:
#   release-rc.sh 7.4.10 rc2 --push

set -euo pipefail

# TODO(release-process): label name is not finalized yet - update this constant once
# the team agrees on it. Every other reference in this script goes through it.
readonly RC_BACKPORT_LABEL="rc-backport"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RELEASE_TAG_SH="$SCRIPT_DIR/release-tag.sh"

function usage {
    echo "Usage:"
    echo "  release-rc.sh <version> <rc#> [--push] [--dry-run] [--debug]"
    echo ""
    echo "  <version>  MAJOR.MINOR.PATCH, e.g. 7.4.10"
    echo "  <rc#>      rc2 or higher, e.g. rc2, rc3"
    echo "             rc1/alpha#/GA/RBA are not handled here - use setversion.sh for those."
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

if [ ! -f "$RELEASE_TAG_SH" ]; then
    echo "Error: $RELEASE_TAG_SH not found."
    echo "release-rc.sh depends on create_and_push_tag() from release-tag.sh (see PR #10571 / RUN-4938)."
    exit 6
fi
# shellcheck source=./release-tag.sh
source "$RELEASE_TAG_SH"  # provides create_and_push_tag() and a dry-run-aware git() wrapper

# Extend the git() wrapper sourced from release-tag.sh with the extra read/write
# commands this script needs (cherry-pick, fetch, cat-file). Redefining the function
# after sourcing is safe - create_and_push_tag() resolves `git` at call time.
function git() {
    if [ "$DRY_RUN" = true ]; then
        case "$1" in
            rev-parse|show-ref|diff|log|status|branch|ls-remote|symbolic-ref|cat-file|rev-list|fetch)
                command git "$@"
                ;;
            tag)
                if [[ " $* " == *" -l "* || " $* " == *" --list"* ]]; then
                    command git "$@"
                else
                    echo "[DRY-RUN] git $*"
                fi
                ;;
            checkout|cherry-pick|push|commit|add)
                echo "[DRY-RUN] git $*"
                return 0
                ;;
            *)
                command git "$@"
                ;;
        esac
    else
        command git "$@"
    fi
}

if [[ ! "$VNUM" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "Error: version ($VNUM) must be in MAJOR.MINOR.PATCH format"
    exit 3
fi

# Only rc2+ is handled here - reject rc1/alpha#/GA/RBA even if the caller's routing
# logic misfires, rather than trusting it blindly.
if [[ ! "$VTAG" =~ ^rc([0-9]+)$ ]]; then
    echo "Error: '$VTAG' is not a valid RC tag type for release-rc.sh. Expected 'rc<N>'."
    echo "GA/alpha#/RBA/rc1 are handled by setversion.sh, not release-rc.sh."
    exit 3
fi
NEW_RC_NUM="${BASH_REMATCH[1]}"

if [ "$NEW_RC_NUM" -lt 2 ]; then
    echo "Error: release-rc.sh only handles rc2 and above. Use setversion.sh for rc1."
    exit 3
fi

VERSION="$VNUM"
NEW_TAG="v$VERSION-$VTAG"
PREV_RC_NUM=$((NEW_RC_NUM - 1))
PREV_TAG="v$VERSION-rc$PREV_RC_NUM"

echo "New RC tag:      $NEW_TAG"
echo "Previous RC tag: $PREV_TAG"
echo "PR label:        $RC_BACKPORT_LABEL"

git fetch --tags --quiet

if ! git rev-parse --verify "refs/tags/$PREV_TAG^{commit}" >/dev/null 2>&1; then
    echo "Error: previous RC tag '$PREV_TAG' not found. Cannot determine base commit."
    exit 4
fi

if git rev-parse --verify "refs/tags/$NEW_TAG^{commit}" >/dev/null 2>&1; then
    echo "Error: tag '$NEW_TAG' already exists."
    exit 5
fi

PREV_COMMIT="$(git rev-parse "refs/tags/$PREV_TAG^{commit}")"
echo "Checking out $PREV_TAG ($PREV_COMMIT) detached"
git checkout --detach "$PREV_COMMIT"

echo "Looking up merged PRs labeled '$RC_BACKPORT_LABEL'..."
PR_DATA="$(gh pr list --label "$RC_BACKPORT_LABEL" --state merged -L 200 \
    --json number,title,mergedAt,mergeCommit \
    --jq 'sort_by(.mergedAt)[] | select(.mergeCommit != null) | [.number, .mergeCommit.oid, .title] | @tsv')"

# Every cherry-pick below uses `-x`, which appends "(cherry picked from commit <sha>)"
# to the resulting commit message. That trailer is what makes "already applied"
# trackable across RC generations: rc3 is cherry-picked on top of rc2, which already
# carries rc1's and rc2's trailers in its history, so grepping $PREV_COMMIT's ancestry
# for a PR's original merge-commit SHA reliably tells us it's already in the lineage -
# comparing the PR's original SHA directly against our history (via merge-base
# --is-ancestor) does NOT work, since cherry-pick always creates a brand new SHA.
ALREADY_APPLIED="$(git log --format=%B "$PREV_COMMIT" | grep -oE '\(cherry picked from commit [0-9a-f]{40}\)' | grep -oE '[0-9a-f]{40}')"

# Per-PR outcome, tab-separated: number / sha / status / detail / title.
# status is one of: already-applied | applied | CONFLICT
REPORT=()
CONFLICT_COUNT=0

if [ -z "$PR_DATA" ]; then
    echo "No merged PRs found with label '$RC_BACKPORT_LABEL'. Nothing to cherry-pick."
else
    mapfile -t PR_LINES <<< "$PR_DATA"
    for line in "${PR_LINES[@]}"; do
        IFS=$'\t' read -r PR_NUM PR_SHA PR_TITLE <<< "$line"

        if grep -qxF "$PR_SHA" <<< "$ALREADY_APPLIED"; then
            echo "PR #$PR_NUM ($PR_SHA): already cherry-picked into a previous RC, skipping"
            REPORT+=("$PR_NUM"$'\t'"$PR_SHA"$'\t'"already-applied"$'\t'"-"$'\t'"$PR_TITLE")
            continue
        fi

        echo "Cherry-picking PR #$PR_NUM ($PR_SHA): $PR_TITLE"

        # A merge-commit-strategy PR has multiple parents; replay only the diff
        # against its first (mainline) parent. Squash/rebase merges are single-parent
        # and cherry-pick normally.
        PARENTS=($(git log -1 --pretty=%P "$PR_SHA"))
        CHERRY_PICK_ARGS=(-x)
        if [ "${#PARENTS[@]}" -gt 1 ]; then
            CHERRY_PICK_ARGS+=(-m 1)
        fi

        if git cherry-pick "${CHERRY_PICK_ARGS[@]}" "$PR_SHA"; then
            REPORT+=("$PR_NUM"$'\t'"$PR_SHA"$'\t'"applied"$'\t'"$(git rev-parse --short HEAD)"$'\t'"$PR_TITLE")
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

FINAL_COMMIT="$(git rev-parse HEAD)"

if [ "$CONFLICT_COUNT" -gt 0 ]; then
    RESCUE_BRANCH="rescue/$NEW_TAG"
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
    echo "  2. For each CONFLICT PR above: git cherry-pick <sha> (add -m 1 if it's a merge commit),"
    echo "     resolve the listed files, git add <files>, git cherry-pick --continue"
    echo "  3. Once every PR is in, tag manually:"
    echo "       $RELEASE_TAG_SH $NEW_TAG \$(git rev-parse HEAD) [--push]"
    exit 7
fi

echo "All labeled PRs applied cleanly. Final commit for $NEW_TAG: $FINAL_COMMIT"
create_and_push_tag "$NEW_TAG" "$FINAL_COMMIT" "Release $VERSION rc$NEW_RC_NUM"
