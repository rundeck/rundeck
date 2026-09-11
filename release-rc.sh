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
# shellcheck source=./release-version.sh
source "$RELEASE_VERSION_SH"  # provides validate_version_format, parse_rc_number, version_tag_name

# Extend the read/write command lists the git() wrapper (sourced from release-tag.sh)
# checks, with the extra commands this script needs. No need to redefine git()
# itself - it re-reads these lists on every call.
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
    --json number,title,mergedAt,mergeCommit,labels \
    --jq 'sort_by(.mergedAt)[] | select(.mergeCommit != null) | [.number, .mergeCommit.oid, .title, ([.labels[].name] | join(","))] | @tsv')"

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
        IFS=$'\t' read -r PR_NUM PR_SHA PR_TITLE PR_LABELS_CSV <<< "$line"

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
            if [ "$DRY_RUN" = true ]; then
                echo "[DRY-RUN] gh pr edit $PR_NUM --add-label $RC_BACKPORT_APPLIED_LABEL"
            else
                gh pr edit "$PR_NUM" --add-label "$RC_BACKPORT_APPLIED_LABEL" \
                    || echo "  Warning: failed to add '$RC_BACKPORT_APPLIED_LABEL' to PR #$PR_NUM - label it manually so future RC runs recognize it's applied"
            fi
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
    echo "     Then label it, regardless of the commit message used to continue -"
    echo "     future RC runs rely on this label, not the commit message, to know it's done:"
    echo "       gh pr edit <PR#> --add-label $RC_BACKPORT_APPLIED_LABEL"
    echo "  3. Once every PR is in, tag manually:"
    echo "       $RELEASE_TAG_SH $NEW_TAG \$(git rev-parse HEAD) [--push]"
    exit 7
fi

echo "All labeled PRs applied cleanly. Final commit for $NEW_TAG: $FINAL_COMMIT"
create_and_push_tag "$NEW_TAG" "$FINAL_COMMIT" "Release $VERSION rc$NEW_RC_NUM"
