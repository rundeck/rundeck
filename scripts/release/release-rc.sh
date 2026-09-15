#!/bin/bash
#
# release-rc.sh - cuts an RC2+ pre-release tag.
#
# Owned by external release tooling per PR #10571 / RUN-4938 (setversion.sh explicitly
# refuses rc2+ and points here) - this script still validates the <version>/<rc#>
# inputs itself rather than trusting the caller's routing.
#
# Invoked by a single Rundeck job, one operator at a time - not designed or tested
# for concurrent/parallel invocation. On a conflict it hands off to a human (see
# handle_conflicts_and_exit) rather than guarding against races with a second run.
#
# Cherry-picks every merged PR labeled $RC_BACKPORT_LABEL, in merge order, onto the
# previous RC tag, regardless of merge strategy (see classify_cherry_pick_target),
# and only tags the result if every one applied - a release tag must represent the
# complete labeled set, never a partial one. Resuming a prior CONFLICT (see
# resolve_base_and_resume/process_labeled_prs) re-verifies rather than re-picks.
#
# Usage:
#   release-rc.sh <version> <rc#> [--push] [--dry-run] [--debug]
#
# Functions share state via globals (PR_NUM, REPORT, ...); execution order is the
# call sequence at the bottom of the file.

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
    echo "             rc1/alpha#/GA are not handled here - use setversion.sh for those."
    echo "             If rescue/<tag> already exists for this <version>/<rc#>, it's finished"
    echo "             (verified, then tagged) instead of starting the backport over."
    echo ""
    echo "Flags:"
    echo "  --push      Push the new tag to origin"
    echo "  --dry-run   Show what would be done without making changes"
    echo "  --debug,-v  Enable verbose debug output"
    exit 2
}

# parse_flags "$@" - splits --push/--dry-run/--debug into PUSH_TO_ORIGIN/DRY_RUN/
# DEBUG, leaving the rest in ARGS. `set --` only affects this function's own
# positional params, so the caller does `set -- "${ARGS[@]}"` right after calling.
function parse_flags {
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
                echo "[DRY-RUN MODE] Rehearses for real: checks out the base commit and cherry-picks onto it (both local-only,"
                echo "left as a detached HEAD with local commits when this exits). No tag, push, rescue branch, or GitHub label is made."
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
}

# validate_and_resolve_version - validates <version>/<rc#> (flags already stripped
# by parse_flags), sources release-tag.sh/release-version.sh, and computes
# NEW_TAG/PREV_TAG/RC_BACKPORT_LABEL/RC_BACKPORT_APPLIED_LABEL.
function validate_and_resolve_version {
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

    # Extends the git() wrapper's read-only list (it re-reads this on every call).
    # `fetch`/`checkout`/`cherry-pick` all run for real under --dry-run: they're
    # local and reversible, and the whole point of a dry run here is to actually
    # attempt the backport and surface a real conflict, not just print intentions.
    RELEASE_GIT_READONLY_CMDS+=" cat-file rev-list fetch checkout"

    validate_version_format "$VNUM" || exit 3

    # Only rc2+ is handled here - reject rc1/alpha#/GA even if the caller's routing
    # logic misfires, rather than trusting it blindly.
    if ! NEW_RC_NUM="$(parse_rc_number "$VTAG")"; then
        echo "Error: '$VTAG' is not a valid RC tag type for release-rc.sh. Expected 'rc<N>'."
        echo "GA/alpha#/rc1 are handled by setversion.sh, not release-rc.sh."
        exit 3
    fi

    if [ "$NEW_RC_NUM" -lt 2 ]; then
        echo "Error: release-rc.sh only handles rc2 and above. Use setversion.sh for rc1."
        exit 3
    fi

    VERSION="$VNUM"
    # Built from $NEW_RC_NUM (already normalized to base-10 by parse_rc_number), not
    # the raw $VTAG: "rc02" would otherwise pass validation as RC 2 but produce tag
    # "v$VERSION-rc02", which the next run's PREV_TAG ("v$VERSION-rc2", built the
    # same normalized way) would never find - silently breaking the lineage.
    NEW_TAG="$(version_tag_name "$VERSION" "rc$NEW_RC_NUM")"
    PREV_RC_NUM=$((NEW_RC_NUM - 1))
    PREV_TAG="$(version_tag_name "$VERSION" "rc$PREV_RC_NUM")"

    # Scoped to this exact version, not a single global label - so two release lines
    # in flight at once (e.g. 6.2.0-rc2 and 6.1.1, or 6.2.0-rc2 and 6.2.1-rc1) can't
    # have a PR meant for one swept into the other's RC. Matches NEW_TAG/PREV_TAG's
    # granularity (full MAJOR.MINOR.PATCH) - major.minor alone would still collide
    # between patch releases of the same minor.
    RC_BACKPORT_LABEL="rc-backport-$VERSION"
    readonly RC_BACKPORT_LABEL

    # Authoritative "already applied" signal, set once a PR has actually landed in
    # this version's RC lineage - the cherry-pick trailer (compute_already_applied)
    # is a secondary signal since a manual conflict resolution can omit it. NOT the
    # existing generic "backport-completed" label - that one belongs to the
    # unrelated post-GA auto-backport GitHub Action.
    RC_BACKPORT_APPLIED_LABEL="rc-backport-$VERSION-applied"
    readonly RC_BACKPORT_APPLIED_LABEL

    echo "New RC tag:      $NEW_TAG"
    echo "Previous RC tag: $PREV_TAG"
    echo "PR label:        $RC_BACKPORT_LABEL"
    echo "Applied label:   $RC_BACKPORT_APPLIED_LABEL"
}

function ensure_applied_label_exists {
    if [ "$DRY_RUN" = true ]; then
        echo "[DRY-RUN] gh label create $RC_BACKPORT_APPLIED_LABEL --force"
    else
        gh label create "$RC_BACKPORT_APPLIED_LABEL" \
            --description "Applied (by release-rc.sh or manually) to the $VERSION RC lineage - unrelated to the generic 'backport-completed' label" \
            --color BFD4F2 --force >/dev/null
    fi
}

# resolve_base_and_resume - fetches tags, refuses if $NEW_TAG already exists,
# resolves $PREV_COMMIT, detects an existing rescue/<tag> branch to resume from
# ($RESUME_COMMIT), and checks out the working commit. Sets $TRAILER_SCAN_BASE.
function resolve_base_and_resume {
    git fetch --tags --quiet

    if git rev-parse --verify "refs/tags/$NEW_TAG^{commit}" >/dev/null 2>&1; then
        echo "Error: tag '$NEW_TAG' already exists."
        exit 5
    fi

    # The previous RC tag is the cherry-pick base.
    if ! git rev-parse --verify "refs/tags/$PREV_TAG^{commit}" >/dev/null 2>&1; then
        echo "Error: previous RC tag '$PREV_TAG' not found. Cannot determine base commit."
        exit 4
    fi
    PREV_COMMIT="$(git rev-parse "refs/tags/$PREV_TAG^{commit}")"

    # A rescue/<tag> branch already existing means a previous run left off here
    # after a CONFLICT - resume from it instead of starting over. Checked on
    # origin first (a previous --push run), then locally. Trusted at face value:
    # the per-PR presence check in process_labeled_prs is the real safety net.
    RESCUE_BRANCH="rescue/$NEW_TAG"
    RESUME_COMMIT=""
    if LS_REMOTE_OUTPUT="$(git ls-remote --heads origin "$RESCUE_BRANCH" 2>&1)" && [ -n "$LS_REMOTE_OUTPUT" ]; then
        git fetch --quiet origin "$RESCUE_BRANCH"
        RESUME_COMMIT="$(git rev-parse FETCH_HEAD)"
    elif git rev-parse --verify "refs/heads/$RESCUE_BRANCH" >/dev/null 2>&1; then
        RESUME_COMMIT="$(git rev-parse "refs/heads/$RESCUE_BRANCH")"
    fi
    if [ -n "$RESUME_COMMIT" ]; then
        echo "Found existing $RESCUE_BRANCH ($RESUME_COMMIT) - resuming from it instead of starting the backport over."
    fi

    if [ -n "$RESUME_COMMIT" ]; then
        TRAILER_SCAN_BASE="$RESUME_COMMIT"
    else
        echo "Checking out $PREV_TAG ($PREV_COMMIT) detached"
        git checkout --detach "$PREV_COMMIT"
        TRAILER_SCAN_BASE="$PREV_COMMIT"
    fi
}

# fetch_labeled_prs - fetches every merged PR labeled $RC_BACKPORT_LABEL as one JSON
# snapshot, validates it, and derives PR_DATA (in merge order) and INITIAL_PR_NUM_SET.
function fetch_labeled_prs {
    echo "Looking up merged PRs labeled '$RC_BACKPORT_LABEL'..."
    # Fetched once as a single JSON snapshot - PR_RAW_COUNT, the null-mergeCommit
    # check, and PR_DATA all derive from this same response, so they can't observe
    # mutually inconsistent label state across separate calls. A second, cheap
    # fetch right before tagging (see verify_pr_set_unchanged) catches the set
    # changing since.
    PR_LABEL_FETCH_LIMIT=1000
    PR_LIST_JSON="$(gh pr list --label "$RC_BACKPORT_LABEL" --state merged -L "$PR_LABEL_FETCH_LIMIT" \
        --json number,title,mergedAt,mergeCommit,labels)"
    PR_RAW_COUNT="$(jq 'length' <<< "$PR_LIST_JSON")"
    if [ "$PR_RAW_COUNT" -ge "$PR_LABEL_FETCH_LIMIT" ]; then
        echo "Error: gh pr list returned $PR_RAW_COUNT PRs labeled '$RC_BACKPORT_LABEL', at or above the fetch"
        echo "limit of $PR_LABEL_FETCH_LIMIT - there may be more matching merged PRs than were fetched, which would let"
        echo "a partial set be tagged as complete. Raise PR_LABEL_FETCH_LIMIT in this script and re-run."
        exit 9
    fi
    # A merged PR can have a null mergeCommit (GitHub hasn't resolved/computed it
    # yet) - the @tsv pipeline below has to filter those out to build valid rows,
    # but doing that silently would let the PR vanish from consideration and tag
    # with it quietly missing. Refuse instead of guessing.
    PR_NULL_MERGE_COMMIT_NUMS="$(jq -r '.[] | select(.mergeCommit == null) | .number' <<< "$PR_LIST_JSON")"
    if [ -n "$PR_NULL_MERGE_COMMIT_NUMS" ]; then
        echo "Error: PR(s) labeled '$RC_BACKPORT_LABEL' have no resolvable merge commit (mergeCommit is null), so they can't be included:"
        while IFS= read -r n; do echo "  #$n"; done <<< "$PR_NULL_MERGE_COMMIT_NUMS"
        echo "Refusing to tag $NEW_TAG while silently dropping them from what must be a complete labeled set."
        echo "Investigate why GitHub reports no merge commit for these (unusual for a merged PR) before re-running."
        exit 13
    fi
    # Commit count is NOT in this snapshot via `--json commits` - gh's GraphQL query
    # for that field is a paginated node list capped well under 200, which would
    # silently undercount a larger PR. Fetched per-PR below instead, from the REST
    # API's uncapped scalar `commits` count field.
    PR_DATA="$(jq -r 'sort_by(.mergedAt)[] | select(.mergeCommit != null) | [.number, .mergeCommit.oid, .title, ([.labels[].name] | join(","))] | @tsv' <<< "$PR_LIST_JSON")"
    # The exact set of PR numbers in this snapshot, compared again immediately
    # before tagging to catch the labeled set changing mid-run.
    INITIAL_PR_NUM_SET="$(jq -r '[.[].number] | sort | @tsv' <<< "$PR_LIST_JSON")"
}

# compute_already_applied - populates $ALREADY_APPLIED with every commit SHA whose
# `-x` cherry-pick trailer appears in $TRAILER_SCAN_BASE's ancestry (a secondary
# signal to $RC_BACKPORT_APPLIED_LABEL - a manual conflict resolution can omit the
# trailer, so both are checked). `|| true` guards the ordinary "no trailers yet"
# case (e.g. the first rc2 of a line) from killing the script via `set -e`.
function compute_already_applied {
    ALREADY_APPLIED="$(git log --format=%B "$TRAILER_SCAN_BASE" | grep -oE '\(cherry picked from commit [0-9a-f]{40}\)' | grep -oE '[0-9a-f]{40}' || true)"
}

# manual_resolve_hint <pr-num> - the standard "couldn't safely classify this PR"
# resume instruction, reused by every classification failure site in
# cherry_pick_one_pr so the wording (and the fact that it always points at
# $RESCUE_BRANCH, not a guessed command) stays consistent.
function manual_resolve_hint {
    echo "Investigate manually - determine the correct commit(s) for #$1 yourself, then on $RESCUE_BRANCH: git cherry-pick -x <correct-sha-or-range>"
}

# mark_pr_conflict <log-msg> <detail> [resume-cmd] - records a per-PR CONFLICT
# (report row, optional resume command, and the counter gating tagging), reading
# $PR_NUM/$PR_SHA/$PR_TITLE from the caller's loop. Caller still does its own
# `continue` right after - easier to see at the call site than inside a helper.
function mark_pr_conflict {
    local log_msg="$1" detail="$2" resume_cmd="${3:-}"
    echo "  $log_msg"
    REPORT+=("$PR_NUM"$'\t'"$PR_SHA"$'\t'"CONFLICT"$'\t'"$detail"$'\t'"$PR_TITLE")
    if [ -n "$resume_cmd" ]; then
        RESUME_CMDS["$PR_NUM"]="$resume_cmd"
    fi
    CONFLICT_COUNT=$((CONFLICT_COUNT + 1))
}

# classify_cherry_pick_target - determines CHERRY_PICK_TARGET/CHERRY_PICK_ARGS for
# the current $PR_NUM/$PR_SHA (merge-commit / squash / rebase-merge, see below).
# Returns 1 (via mark_pr_conflict) if the PR can't be safely classified at all.
function classify_cherry_pick_target {
    CHERRY_PICK_TARGET="$PR_SHA"
    CHERRY_PICK_ARGS=(-x)

    # A merge-commit-strategy PR has multiple parents; replay only the diff
    # against its first (mainline) parent - that one commit's diff against
    # mainline is always the PR's whole diff, regardless of how many commits
    # the PR had, so a single cherry-pick is always correct here.
    PARENTS=($(git log -1 --pretty=%P "$PR_SHA"))
    if [ "${#PARENTS[@]}" -gt 1 ]; then
        CHERRY_PICK_ARGS+=(-m 1)
        return 0
    fi

    # Fetched fresh here from the REST API's uncapped scalar `commits` field
    # (unlike the paginated `--json commits` used in fetch_labeled_prs).
    # Guarded the same way as the resolvability check in cherry_pick_one_pr: a
    # transient API failure here becomes a CONFLICT for just this PR, not a crash.
    if ! PR_COMMIT_COUNT="$(gh api "repos/{owner}/{repo}/pulls/$PR_NUM" --jq '.commits' 2>&1)"; then
        mark_pr_conflict \
            "Could not fetch commit count for PR #$PR_NUM from GitHub (transient API failure?) - can't safely tell squash from rebase-merge. Marking as CONFLICT." \
            "commit-count lookup failed - resolve manually on $RESCUE_BRANCH" \
            "$(manual_resolve_hint "$PR_NUM")"
        return 1
    fi
    if [ "${PR_COMMIT_COUNT:-1}" -le 1 ]; then
        return 0
    fi

    # Single-parent PRs with more than one original commit are ambiguous:
    # squash-merge folds every commit into this one (single cherry-pick is
    # correct), but rebase-merge replays each original commit individually
    # onto the base and mergeCommit is only the LAST of that chain -
    # cherry-picking just the tip would silently drop earlier commits.
    # (Verified against a real case: a 4-commit rebase-merged PR's mergeCommit
    # alone touched only 1 of the 4 changed files.) Parent count can't
    # distinguish squash from rebase-merge (both are single-parent), so compare
    # the tip's patch-id against the full PR diff's: match -> squash, mismatch
    # -> rebase-merge, cherry-pick the range of the last PR_COMMIT_COUNT
    # commits instead of just the tip.
    if ! FULL_PR_PATCH_ID="$(gh pr diff "$PR_NUM" | git patch-id --stable | awk '{print $1}')"; then
        mark_pr_conflict \
            "Could not compute the full PR diff's patch-id for #$PR_NUM (gh pr diff failed?) - can't safely verify squash vs. rebase-merge. Marking as CONFLICT." \
            "patch-id computation failed - resolve manually on $RESCUE_BRANCH" \
            "$(manual_resolve_hint "$PR_NUM")"
        return 1
    fi
    if ! TIP_PATCH_ID="$(git diff "$PR_SHA"^ "$PR_SHA" | git patch-id --stable | awk '{print $1}')"; then
        mark_pr_conflict \
            "Could not compute the tip commit's patch-id for #$PR_NUM. Marking as CONFLICT." \
            "patch-id computation failed - resolve manually on $RESCUE_BRANCH" \
            "$(manual_resolve_hint "$PR_NUM")"
        return 1
    fi
    if [ -z "$FULL_PR_PATCH_ID" ]; then
        # Net-empty full PR diff (e.g. a rebase-merged PR whose commits cancel
        # out overall) - nothing to verify a range against, so require manual
        # classification rather than falling through to a (possibly wrong)
        # single tip cherry-pick.
        mark_pr_conflict \
            "PR #$PR_NUM: the full PR diff's patch-id came back empty (net-empty aggregate diff?) - can't safely verify squash vs. rebase-merge against it. Marking as CONFLICT for manual classification." \
            "full PR diff patch-id empty - manual classification needed" \
            "$(manual_resolve_hint "$PR_NUM")"
        return 1
    fi
    if [ "$FULL_PR_PATCH_ID" = "$TIP_PATCH_ID" ]; then
        return 0
    fi

    # A tip mismatch alone doesn't prove rebase-merge - GitHub's PR diff and a
    # local `git diff` can differ for unrelated reasons (rename detection,
    # whitespace), which could misclassify a squash PR. Verify the computed
    # range's own diff also matches the full PR diff before trusting it; if
    # not, refuse and require manual classification rather than guess and risk
    # sweeping in unrelated mainline history via RANGE_BASE.
    RANGE_BASE="$PR_SHA~$PR_COMMIT_COUNT"
    if ! git rev-parse --verify "${RANGE_BASE}^{commit}" >/dev/null 2>&1; then
        mark_pr_conflict \
            "Detected rebase-merge of $PR_COMMIT_COUNT commits, but $RANGE_BASE isn't reachable (shallow history?) - refusing to cherry-pick only the tip. Marking as CONFLICT." \
            "range base $RANGE_BASE unreachable - fetch full history" \
            "git fetch --unshallow (or otherwise deepen history) then: git cherry-pick -x ${RANGE_BASE}..${PR_SHA}"
        return 1
    fi
    RANGE_PATCH_ID="$(git diff "$RANGE_BASE" "$PR_SHA" | git patch-id --stable | awk '{print $1}' || true)"
    if [ -n "$RANGE_PATCH_ID" ] && [ "$RANGE_PATCH_ID" = "$FULL_PR_PATCH_ID" ]; then
        echo "  Detected rebase-merge of $PR_COMMIT_COUNT commits (mergeCommit alone doesn't match the full PR diff, but the full range does) - cherry-picking $RANGE_BASE..$PR_SHA instead of just the tip"
        CHERRY_PICK_TARGET="${RANGE_BASE}..${PR_SHA}"
        return 0
    fi
    mark_pr_conflict \
        "PR #$PR_NUM: mergeCommit alone doesn't match the full PR diff, and neither does the computed $PR_COMMIT_COUNT-commit range - can't safely tell rebase-merge from some other discrepancy. Refusing to guess. Marking as CONFLICT for manual classification." \
        "neither tip nor computed range matches the full PR diff - manual classification needed" \
        "$(manual_resolve_hint "$PR_NUM")"
    return 1
}

# cherry_pick_one_pr - handles a single not-yet-applied PR: verifies $PR_SHA is
# resolvable, classifies and attempts the cherry-pick, and records the outcome.
function cherry_pick_one_pr {
    if [ -n "$RESUME_COMMIT" ]; then
        # Resuming only ever verifies - it never cherry-picks onto $RESUME_COMMIT,
        # since that would mean guessing at a base HEAD isn't even checked out
        # to. A PR that isn't already-applied here is a genuine gap in the
        # rescue branch, not something to fix by picking onto an unrelated tree.
        mark_pr_conflict \
            "PR #$PR_NUM ($PR_SHA): NOT found at $RESUME_COMMIT (no '$RC_BACKPORT_APPLIED_LABEL' label, no matching cherry-pick trailer)" \
            "missing from $RESCUE_BRANCH"
        return
    fi

    echo "Cherry-picking PR #$PR_NUM ($PR_SHA): $PR_TITLE"

    # Verified resolvable before anything else touches it: a bare command
    # substitution inside classify_cherry_pick_target isn't exempt from `set -e`,
    # so an unresolvable SHA (shallow/partial clone, pruned object) would otherwise
    # kill the whole run. Also means parent count/merge strategy can't be
    # determined, so there's no safe automatic classification either way.
    if ! git rev-parse --verify "${PR_SHA}^{commit}" >/dev/null 2>&1; then
        mark_pr_conflict \
            "PR #$PR_NUM references commit $PR_SHA, which isn't resolvable in this checkout (shallow clone? pruned?). Marking as CONFLICT." \
            "commit not resolvable locally - resolve manually on $RESCUE_BRANCH" \
            "$(manual_resolve_hint "$PR_NUM")"
        return
    fi

    if ! classify_cherry_pick_target; then
        return
    fi

    RESUME_CMDS["$PR_NUM"]="git cherry-pick ${CHERRY_PICK_ARGS[*]} $CHERRY_PICK_TARGET"

    if git cherry-pick "${CHERRY_PICK_ARGS[@]}" "$CHERRY_PICK_TARGET"; then
        REPORT+=("$PR_NUM"$'\t'"$PR_SHA"$'\t'"applied"$'\t'"$(git rev-parse --short HEAD)"$'\t'"$PR_TITLE")
        PRS_TO_LABEL+=("$PR_NUM")
    else
        # Deliberately not auto-resolving (e.g. -X ours/theirs) - guessing wrong on
        # a release cherry-pick ships a silent regression. Abort just this PR and
        # keep going, so one conflict doesn't hide the status of the rest.
        CONFLICT_FILES="$(git diff --name-only --diff-filter=U | paste -sd ',' -)"
        # `--abort` can itself fail if the pick never reached a resumable state
        # (e.g. a bad revision) - `|| true` keeps that from killing the whole
        # run via `set -e` and losing the report for every PR already applied.
        git cherry-pick --abort || echo "  Warning: 'git cherry-pick --abort' itself failed for PR #$PR_NUM - continuing anyway; verify the working tree isn't left in a partial state."
        echo "  conflict on: ${CONFLICT_FILES:-<none captured - cherry-pick may have failed before leaving a conflict state>} - left out, continuing with the rest"
        REPORT+=("$PR_NUM"$'\t'"$PR_SHA"$'\t'"CONFLICT"$'\t'"$CONFLICT_FILES"$'\t'"$PR_TITLE")
        CONFLICT_COUNT=$((CONFLICT_COUNT + 1))
    fi
}

# process_labeled_prs - iterates $PR_DATA, skipping already-applied PRs
# (backfilling the applied label where only the trailer caught it) and
# cherry-picking (or, in resume mode, just verifying) the rest via
# cherry_pick_one_pr. Populates $REPORT/$PRS_TO_LABEL/$CONFLICT_COUNT.
function process_labeled_prs {
    REPORT=()
    CONFLICT_COUNT=0
    # Deferred to label_applied_prs, never labeled as each PR is found: if a LATER
    # PR in this run conflicts, the run aborts without tagging, but an early label
    # would let a rerun trust it and skip a PR whose commits never actually landed.
    PRS_TO_LABEL=()
    # PR number -> exact resume command (single-commit vs range needs a different form).
    declare -gA RESUME_CMDS

    if [ -z "$PR_DATA" ]; then
        echo "Error: No merged PRs found with label '$RC_BACKPORT_LABEL'. Refusing to cut $NEW_TAG with nothing to backport -"
        echo "this almost always means the wrong version was labeled, or nothing was labeled yet, not that $NEW_TAG is"
        echo "genuinely a no-op re-tag of $TRAILER_SCAN_BASE."
        echo "If a no-op re-tag is really what's intended, do it explicitly instead of through this script:"
        echo "  $RELEASE_TAG_SH $NEW_TAG $TRAILER_SCAN_BASE [--push]"
        exit 8
    fi

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
            # Backfill the label if only the trailer caught it - same deferred-until-tagged
            # handling as PRS_TO_LABEL above.
            if [ "$ALREADY_LABELED" = false ]; then
                PRS_TO_LABEL+=("$PR_NUM")
            fi
            continue
        fi

        cherry_pick_one_pr
    done
}

function print_report {
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
}

# handle_conflicts_and_exit - if any PR is still CONFLICT/missing, saves what did
# apply to a rescue branch and exits 7 with resolve instructions. Returns without
# exiting if $CONFLICT_COUNT is 0.
function handle_conflicts_and_exit {
    if [ "$CONFLICT_COUNT" -eq 0 ]; then
        return
    fi

    if [ -n "$RESUME_COMMIT" ]; then
        # Resuming never cherry-picks, so there's nothing new to save - point back
        # at $RESCUE_BRANCH instead of restating cherry-pick instructions.
        echo ""
        echo "Error: $RESCUE_BRANCH ($RESUME_COMMIT) is still missing $CONFLICT_COUNT PR(s) marked CONFLICT above."
        echo "Refusing to tag $NEW_TAG - a release tag must include every PR labeled '$RC_BACKPORT_LABEL', never a partial set."
        echo ""
        echo "To resolve: on $RESCUE_BRANCH, cherry-pick the still-missing PR(s), push the branch FIRST,"
        echo "then label each one $RC_BACKPORT_APPLIED_LABEL (not before pushing - a run started elsewhere"
        echo "would otherwise trust the label for a commit that doesn't durably exist anywhere yet), then"
        echo "re-run this same command."
        exit 7
    fi

    if [ "$DRY_RUN" = true ]; then
        echo "[DRY-RUN] git branch $RESCUE_BRANCH HEAD"
    else
        # Not force-created: reaching here with a same-name branch already present
        # means separate manual activity - fail loudly rather than clobber it.
        if ! command git branch "$RESCUE_BRANCH" "$FINAL_COMMIT"; then
            echo "Error: local branch '$RESCUE_BRANCH' already exists and doesn't match this run's candidate - not overwriting it."
            echo "Investigate what's on it, then either remove it or re-run this command - do not tag it directly via"
            echo "$RELEASE_TAG_SH, which has no knowledge of the labeled-PR completeness check this script enforces."
            exit 11
        fi
        echo "Every PR marked 'applied' above is saved to local branch '$RESCUE_BRANCH' ($FINAL_COMMIT) - none of that work is lost."
        if [ "$PUSH_TO_ORIGIN" = true ]; then
            if ! command git push origin "$RESCUE_BRANCH"; then
                echo "Error: failed to push '$RESCUE_BRANCH' to origin - it likely already exists there with different content."
                echo "Investigate before retrying."
                exit 11
            fi
            echo "Also pushed to origin/$RESCUE_BRANCH"
        fi
    fi

    echo ""
    echo "Error: $CONFLICT_COUNT PR(s) marked CONFLICT above could not be cherry-picked cleanly."
    echo "Refusing to tag $NEW_TAG - a release tag must include every PR labeled '$RC_BACKPORT_LABEL', never a partial set."
    echo ""
    if [ "$DRY_RUN" = true ]; then
        # $RESCUE_BRANCH was only ever printed above, never actually created.
        echo "This was a --dry-run: $RESCUE_BRANCH was NOT actually created, nothing below is real yet."
        echo "Re-run this same command without --dry-run to actually attempt the backport and create it, then:"
        echo ""
    fi
    echo "To resolve:"
    echo "  1. git checkout $RESCUE_BRANCH"
    echo "  2. For each CONFLICT PR above, run its specific command (single-commit vs. range differs"
    echo "     per PR - using the wrong one can drop commits), resolve the listed files, git add <files>,"
    echo "     git cherry-pick --continue - but do NOT label anything yet:"
    for entry in "${REPORT[@]}"; do
        IFS=$'\t' read -r R_NUM R_SHA R_STATUS R_DETAIL R_TITLE <<< "$entry"
        if [ "$R_STATUS" = "CONFLICT" ]; then
            echo "       PR #$R_NUM: ${RESUME_CMDS[$R_NUM]:-git cherry-pick -x $R_SHA}"
        fi
    done
    echo "  3. Once every PR is in, push $RESCUE_BRANCH FIRST:"
    echo "       git push origin $RESCUE_BRANCH"
    echo "  4. Only THEN label each PR you just resolved by hand, regardless of the commit message used"
    echo "     to continue - future RC runs rely on this label, not the commit message, to know it's done."
    echo "     Labeling before the push would let a run started elsewhere trust the label for a commit"
    echo "     that doesn't durably exist anywhere yet:"
    echo "       gh pr edit <PR#> --add-label $RC_BACKPORT_APPLIED_LABEL"
    echo "  5. Re-run this same command - it will find the (now-pushed) branch and finish by tagging it,"
    echo "     instead of starting over."
    exit 7
}

# verify_pr_set_unchanged - re-fetches just the labeled PR numbers and refuses to
# proceed if the set has drifted since fetch_labeled_prs' snapshot (the cherry-pick
# loop above can take a while).
function verify_pr_set_unchanged {
    if ! CURRENT_PR_NUM_SET="$(gh pr list --label "$RC_BACKPORT_LABEL" --state merged -L "$PR_LABEL_FETCH_LIMIT" \
        --json number --jq '[.[].number] | sort | @tsv')"; then
        echo "Error: could not re-fetch the labeled PR set from GitHub (transient API failure?) to verify it hasn't"
        echo "changed since this run started. Refusing to tag $NEW_TAG without that check. Re-run once GitHub API access is working."
        exit 14
    fi
    if [ "$CURRENT_PR_NUM_SET" != "$INITIAL_PR_NUM_SET" ]; then
        echo "Error: the set of PRs labeled '$RC_BACKPORT_LABEL' changed since this run started"
        echo "(one was labeled, unlabeled, or newly merged in the meantime) - refusing to tag $NEW_TAG"
        echo "against a stale snapshot. Re-run to pick up the current set."
        exit 14
    fi
}

# label_applied_prs - labels every PR in $PRS_TO_LABEL $RC_BACKPORT_APPLIED_LABEL,
# but only once $NEW_TAG genuinely exists (see finalize_tag) and only when --push
# was used (a local-only tag isn't durable, so labeling now would let a later run
# elsewhere trust the label for backports that never actually reached origin).
function label_applied_prs {
    if [ "${#PRS_TO_LABEL[@]}" -eq 0 ]; then
        return
    fi
    echo ""
    # --push checked before --dry-run so a dry run without --push reports the
    # same "nothing labeled" outcome the real invocation would, not "would label".
    if [ "$PUSH_TO_ORIGIN" = true ]; then
        echo "Labeling PRs '$RC_BACKPORT_APPLIED_LABEL' now that $NEW_TAG exists..."
        for PR_NUM in "${PRS_TO_LABEL[@]}"; do
            if [ "$DRY_RUN" = true ]; then
                echo "[DRY-RUN] gh pr edit $PR_NUM --add-label $RC_BACKPORT_APPLIED_LABEL"
            else
                gh pr edit "$PR_NUM" --add-label "$RC_BACKPORT_APPLIED_LABEL" \
                    || echo "  Warning: failed to add '$RC_BACKPORT_APPLIED_LABEL' to PR #$PR_NUM - label it manually so future RC runs recognize it's applied"
            fi
        done
        return
    fi

    # Not "just re-run with --push": $NEW_TAG exists locally now, so a re-run
    # would fail at the tag-already-exists check - pushing this exact tag and
    # labeling directly is the only way to finish. Dry-run needs its own wording:
    # create_and_push_tag() never actually created $NEW_TAG under --dry-run either.
    if [ "$DRY_RUN" = true ]; then
        echo "Not labeling any PR - this was a --dry-run, so $NEW_TAG was never actually created; nothing here is real yet."
        echo "A real run with the same flags (no --push) would create $NEW_TAG locally only, and not label these PRs either -"
        echo "to make that durable, you'd then need:"
    else
        echo "Not labeling any PR - $NEW_TAG only exists locally without --push."
        echo "Re-running will NOT work now (it will fail - $NEW_TAG already exists locally). To make this durable instead:"
    fi
    echo "  git push origin $NEW_TAG"
    for PR_NUM in "${PRS_TO_LABEL[@]}"; do
        echo "  gh pr edit $PR_NUM --add-label $RC_BACKPORT_APPLIED_LABEL"
    done
}

# cleanup_rescue_branch - deletes $RESCUE_BRANCH (best-effort, local and, with
# --push, on origin) now that $NEW_TAG is the durable record. Runs whether this
# invocation resumed from the branch or created it fresh moments earlier.
function cleanup_rescue_branch {
    if git rev-parse --verify "refs/heads/$RESCUE_BRANCH" >/dev/null 2>&1; then
        # HEAD can still be on $RESCUE_BRANCH (the documented manual-resolve flow
        # checks it out) - `branch -D` refuses to delete the current branch, so
        # detach first, to $FINAL_COMMIT (that branch's own tip - loses nothing).
        CURRENT_BRANCH="$(git symbolic-ref --quiet --short HEAD || true)"
        if [ "$CURRENT_BRANCH" = "$RESCUE_BRANCH" ]; then
            git checkout --detach "$FINAL_COMMIT"
        fi
        if [ "$DRY_RUN" = true ]; then
            echo "[DRY-RUN] git branch -D $RESCUE_BRANCH"
        else
            command git branch -D "$RESCUE_BRANCH" >/dev/null 2>&1 \
                && echo "Deleted local branch $RESCUE_BRANCH (superseded by $NEW_TAG)." \
                || echo "  Warning: failed to delete local branch $RESCUE_BRANCH - remove it manually."
        fi
    fi
    if [ "$PUSH_TO_ORIGIN" = true ] && git ls-remote --exit-code --heads origin "$RESCUE_BRANCH" >/dev/null 2>&1; then
        if [ "$DRY_RUN" = true ]; then
            echo "[DRY-RUN] git push origin --delete $RESCUE_BRANCH"
        else
            command git push origin --delete "$RESCUE_BRANCH" >/dev/null 2>&1 \
                && echo "Deleted origin/$RESCUE_BRANCH (superseded by $NEW_TAG)." \
                || echo "  Warning: failed to delete origin/$RESCUE_BRANCH - remove it manually."
        fi
    fi
}

# finalize_tag - creates (and optionally pushes) $NEW_TAG at $FINAL_COMMIT, then
# labels PRs and cleans up the rescue branch. Propagates create_and_push_tag's
# exit status if tag creation itself fails.
function finalize_tag {
    echo "All labeled PRs verified present. Final commit for $NEW_TAG: $FINAL_COMMIT"
    # Not `if ! create_and_push_tag ...; then exit $?; fi`: `!` negates the exit
    # status the `if` tests, so `$?` inside that then-block is 0 (the negated,
    # "true" status), not create_and_push_tag's real failure code - `exit $?`
    # there would always report success even when tagging/pushing failed.
    if create_and_push_tag "$NEW_TAG" "$FINAL_COMMIT" "Release $VERSION rc$NEW_RC_NUM"; then
        # Only now that $NEW_TAG genuinely exists is anything in PRS_TO_LABEL
        # (fresh cherry-picks and trailer-only backfills alike) actually
        # labeled, and the rescue branch cleaned up.
        label_applied_prs
        cleanup_rescue_branch
    else
        exit $?
    fi
}

# --- main ---
if [ -z "${1:-}" ] || [ -z "${2:-}" ]; then
    usage
fi
parse_flags "$@"
set -- "${ARGS[@]}"

validate_and_resolve_version "$@"
ensure_applied_label_exists
resolve_base_and_resume
fetch_labeled_prs
compute_already_applied
process_labeled_prs
print_report
handle_conflicts_and_exit
verify_pr_set_unchanged
finalize_tag
