#!/bin/bash
#
# release-tag.sh - creates (and optionally pushes) a git tag at a specific commit.
#
# This is the single mechanism used to cut release tags. It has no opinion about GA/rc/alpha -
# the caller resolves the tag name and target commit and hands them here. Shared by:
#   - setversion.sh, for GA / rc1 / alpha# (it resolves tag name + commit itself)
#   - external release tooling, for rc2+ (check rdcore)
#
# Can be sourced for its create_and_push_tag() function (callers set PUSH_TO_ORIGIN/DRY_RUN
# before calling), or invoked directly as a CLI:
#   release-tag.sh <tag-name> <commit> [--push] [--dry-run] [--debug]

: "${PUSH_TO_ORIGIN:=false}"
: "${DRY_RUN:=false}"

# Git wrapper function for dry-run support
function git() {
    if [ "$DRY_RUN" = true ]; then
        case "$1" in
            # Read-only commands - safe to execute
            rev-parse|show-ref|diff|log|status|branch|ls-remote|symbolic-ref)
                command git "$@"
                ;;
            # `git tag` with no args, or with -l/--list, is a read-only listing.
            # Anything else (creating a tag, -d/--delete, etc.) is a write.
            tag)
                shift
                if [ $# -eq 0 ] || [[ " $* " == *" -l "* || " $* " == *" --list"* ]]; then
                    command git tag "$@"
                else
                    echo "[DRY-RUN] git tag $*"
                    return 0
                fi
                ;;
            # Write commands - just show what would be done
            checkout|push|commit|add)
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

# create_and_push_tag <tag-name> <commit>
#
# Creates a tag named <tag-name> pointing at <commit>, and pushes it if
# PUSH_TO_ORIGIN=true. Does not resolve branches, does not know about GA/rc/alpha -
# the caller is responsible for deciding what <commit> should be.
function create_and_push_tag {
    local TAG_NAME="$1"
    local COMMIT_REF="$2"

    if [ -z "$TAG_NAME" ] || [ -z "$COMMIT_REF" ]; then
        echo "Error: create_and_push_tag requires a tag name and a commit"
        return 5
    fi

    if ! git rev-parse --verify "${COMMIT_REF}^{commit}" >/dev/null 2>&1; then
        echo "Error: Commit '$COMMIT_REF' not found in repository"
        return 5
    fi
    local TARGET_COMMIT
    TARGET_COMMIT="$(git rev-parse "${COMMIT_REF}^{commit}")"

    echo "Creating tag: $TAG_NAME"
    if ! git tag "$TAG_NAME" "$TARGET_COMMIT"; then
        echo "Error: Failed to create tag $TAG_NAME"
        return 1
    fi
    echo "Tag created: $TAG_NAME"

    if [ "$PUSH_TO_ORIGIN" = true ]; then
        echo "Pushing tag to remote..."
        if ! git push origin "$TAG_NAME"; then
            echo "Error: Failed to push tag $TAG_NAME"
            return 1
        fi
        echo "Tag pushed to remote."
    else
        echo "Use 'git push origin $TAG_NAME' to push the tag to remote."
    fi
}

# Allow running this file directly as a CLI, not just sourcing it for the function
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    function usage {
        echo "Usage: release-tag.sh <tag-name> <commit> [--push] [--dry-run] [--debug]"
        exit 2
    }

    if [ -z "$1" ]; then
        usage
    fi

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
                set -x
                ;;
            *)
                ARGS+=("$arg")
                ;;
        esac
    done
    set -- "${ARGS[@]}"

    if [ -z "$2" ]; then
        usage
    fi

    create_and_push_tag "$1" "$2"
fi
