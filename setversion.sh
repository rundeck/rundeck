#!/bin/bash

CUR_VERSION="$(grep version.number= "$PWD/version.properties" | cut -d= -f 2)"
CUR_TAG="$(grep version.tag= "$PWD/version.properties" | cut -d= -f 2)"

echo "current NUMBER: $CUR_VERSION"
echo "current TAG: $CUR_TAG"

function usage {
    echo "Usage:"
    echo "  setversion.sh <version> [GA|rc#|alpha#]                                              - Update version in version.properties"
    echo "  setversion.sh --bump-minor                                                           - Bump minor version number"
    echo "  setversion.sh --tag <version> GA [--push] [--dry-run] [--debug]                     - Re-tag the highest existing v<version>-rcN tag as GA (no commit argument)"
    echo "  setversion.sh --tag <version> rc1 <commit> [--push] [--dry-run] [--debug]           - Tag rc1 at an explicit commit (no release branch exists yet)"
    echo "  setversion.sh --tag <version> alpha# <commit> [--push] [--dry-run] [--debug]         - Tag other pre-releases at an explicit commit"
    echo "  (rc2+ is NOT handled by setversion.sh - it is owned by external release tooling, for rc2+ (check rdcore))"
    echo "  setversion.sh --create-release-branch <version> [<commit>] [--push] [--dry-run] [--debug]     - Create release branch for patch releases (branches from GA tag or specified commit)"
    echo ""
    echo "Flags:"
    echo "  --push      Push changes to remote repository"
    echo "  --dry-run   Show what would be done without making changes"
    echo "  --debug,-v  Enable verbose debug output"
    exit 2
}

if [ -z "$1" ] ; then
    usage
fi

# Parse flags (--push, --dry-run, --debug)
PUSH_TO_ORIGIN=false
DRY_RUN=false
DEBUG=false

# Check for flags in any position
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
            set -x  # Enable bash debug mode
            ;;
        *)
            ARGS+=("$arg")
            ;;
    esac
done
set -- "${ARGS[@]}"  # Reset positional parameters without flags

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=release-tag.sh
source "$SCRIPT_DIR/release-tag.sh"  # provides create_and_push_tag() and the dry-run git() wrapper

# Handle tag creation directly on main
if [ "$1" == "--tag" ]; then
    shift

    if [ -z "$1" ] ; then
        echo "Error: Version number required"
        usage
    fi
    VNUM="$1"
    shift
    VTAG="${1:-GA}"
    [ $# -gt 0 ] && shift

    # rc2+ is not handled here - checking out an existing release branch and tagging its HEAD
    # is owned by external release tooling, for rc2+ (check rdcore), which calls release-tag.sh
    # directly once it has resolved the branch and commit itself.
    if [[ "$VTAG" =~ ^rc([0-9]+)$ ]] && [ "${BASH_REMATCH[1]}" -ge 2 ]; then
        echo "Error: rc2+ releases are not created by setversion.sh."
        echo "That flow is owned by external release tooling, for rc2+ (check rdcore)."
        exit 13
    fi

    # Resolve the tag name and target commit for the release type; the actual tag
    # creation/push is delegated to create_and_push_tag (release-tag.sh).
    if [ "$VTAG" == "GA" ]; then
        if [ -n "$1" ]; then
            echo "Error: GA does not take a commit argument - it re-tags the highest existing rc for the version."
            exit 5
        fi

        TAG_NAME="v$VNUM"

        # GA is cut by re-tagging the highest existing RC for this version - never a fresh commit on main
        HIGHEST_RC=""
        HIGHEST_RC_NUM=-1
        for RC_TAG in $(git tag -l "v${VNUM}-rc*"); do
            RC_SUFFIX="${RC_TAG#v${VNUM}-rc}"
            if [[ "$RC_SUFFIX" =~ ^[0-9]+$ ]] && [ "$RC_SUFFIX" -gt "$HIGHEST_RC_NUM" ]; then
                HIGHEST_RC_NUM="$RC_SUFFIX"
                HIGHEST_RC="$RC_TAG"
            fi
        done

        if [ -z "$HIGHEST_RC" ]; then
            echo "Error: No RC tag found matching v$VNUM-rcN. GA is cut by re-tagging the highest RC."
            echo "Tag rc1 first with: setversion.sh --tag $VNUM rc1 <commit>"
            exit 12
        fi

        TARGET_COMMIT="$HIGHEST_RC"
        echo "Re-tagging highest RC $HIGHEST_RC as GA"
    elif [[ "$VTAG" =~ ^[a-z]+[0-9]+$ ]]; then
        # rc1, alpha3, etc. - always tag an explicit commit, never a bare HEAD
        TAG_NAME="v$VNUM-$VTAG"
        COMMIT_ARG="$1"
        if [ -z "$COMMIT_ARG" ]; then
            echo "Error: '$VTAG' requires an explicit commit to tag."
            echo "Usage: setversion.sh --tag $VNUM $VTAG <commit> [--push]"
            exit 5
        fi
        shift
        TARGET_COMMIT="$COMMIT_ARG"
    else
        echo "Error: Invalid tag format '$VTAG'. Expected 'GA' or to match [a-z]+[0-9]+ (e.g., rc1, alpha3)."
        exit 5
    fi

    create_and_push_tag "$TAG_NAME" "$TARGET_COMMIT"
    exit $?

# Create a release branch for patch releases
elif [ "$1" == "--create-release-branch" ]; then
    shift

    if [ -z "$1" ] ; then
        echo "Error: Version required"
        usage
    fi
    VNUM="$1"
    shift
    # Extract major.minor and patch
    IFS='.' read -r MAJOR MINOR PATCH <<< "$VNUM"
    if [ -z "$MAJOR" ] || [ -z "$MINOR" ] || [ -z "$PATCH" ]; then
        echo "Error: Version ($VNUM) must be in MAJOR.MINOR.PATCH format (e.g., 5.19.1)"
        exit 3
    fi

    BASE_VERSION="$MAJOR.$MINOR.0"
    BRANCH_NAME="release/$MAJOR.$MINOR.x"
    GA_TAG="v$BASE_VERSION"

    # Optional commit ref: branch from this instead of the GA tag
    COMMIT_REF="$1"
    [ $# -gt 0 ] && shift
    if [ -n "$1" ]; then
        echo "Error: Unexpected argument '$1'"
        usage
    fi

    # Determine the base ref to branch from
    if [ -n "$COMMIT_REF" ]; then
        if ! git rev-parse --verify "${COMMIT_REF}^{commit}" >/dev/null 2>&1; then
            echo "Error: Commit '$COMMIT_REF' not found in repository"
            exit 6
        fi
        BASE_REF="$COMMIT_REF"
        echo "Creating release branch $BRANCH_NAME from commit $COMMIT_REF"
    else
        if ! git rev-parse --verify "$GA_TAG" >/dev/null 2>&1; then
            echo "Error: GA tag $GA_TAG not found. The release branch should be created from the GA tag."
            echo "Verify that the GA release has been tagged correctly first."
            exit 6
        fi
        BASE_REF="$GA_TAG"
        echo "Creating release branch $BRANCH_NAME from tag $GA_TAG"
    fi

    # Check if branch already exists locally or remotely
    if git rev-parse --verify "$BRANCH_NAME" >/dev/null 2>&1 ||
       git ls-remote --heads origin "$BRANCH_NAME" | grep -q "refs/heads/${BRANCH_NAME}$"; then
        echo "Error: Branch $BRANCH_NAME already exists locally or remotely."
        echo "Use the tag process to create tags on the existing release branch."
        exit 11
    fi

    git checkout -b "$BRANCH_NAME" "$BASE_REF" || exit 7

    # Update version in version.properties to patch-SNAPSHOT
    VDATE="$(date +%Y%m%d)"
    SNAPSHOT_VERSION="$VNUM-SNAPSHOT-$VDATE"
    echo "Setting version to $SNAPSHOT_VERSION in version.properties"

    if [ "$DRY_RUN" = false ]; then
        perl -i'.orig' -p -e "s#^version\\.number\\s*=.*\$#version.number=$VNUM#" "$PWD/version.properties"
        perl -i'.orig' -p -e "s#^version\\.tag\\s*=.*\$#version.tag=SNAPSHOT#" "$PWD/version.properties"
        perl -i'.orig' -p -e "s#^version\\.date\\s*=.*\$#version.date=$VDATE#" "$PWD/version.properties"
        perl -i'.orig' -p -e "s#^version\\.version\\s*=.*\$#version.version=$SNAPSHOT_VERSION#" "$PWD/version.properties"
        echo "Modified: $(pwd)/version.properties"
    else
        echo "[DRY-RUN] Would modify: $(pwd)/version.properties"
    fi

    # Commit the version change
    git add version.properties
    if git diff --cached --name-only | grep -q "version.properties"; then
        echo "Committing version change"
        git commit -m "Set version to $SNAPSHOT_VERSION for patch release"
        echo "Changes committed"
    else
        echo "No changes to commit in version.properties"
    fi

    echo ""
    echo "Release branch $BRANCH_NAME created successfully."

    if [ "$PUSH_TO_ORIGIN" = true ]; then
        echo "Pushing branch to remote..."
        git push origin "$BRANCH_NAME"
        echo "Branch pushed to remote."
    else
        echo "Use 'git push origin $BRANCH_NAME' to push to remote."
    fi
    exit 0

# Handle bump minor version
elif [ "$1" == "--bump-minor" ]; then
  IFS='.' read -r MAJOR MINOR PATCH <<< "$CUR_VERSION"
  if [ -z "$MAJOR" ] || [ -z "$MINOR" ] || [ -z "$PATCH" ]; then
    echo "Error: Current version ($CUR_VERSION) is not in MAJOR.MINOR.PATCH format"
    exit 3
  fi
  MINOR=$((MINOR + 1))
  PATCH=0
  VNUM="$MAJOR.$MINOR.$PATCH"
  VTAG="$CUR_TAG"
  shift
else
  VNUM="$1"
  shift
  VTAG="${1:-GA}"
fi

VDATE="$(date +%Y%m%d)"

if [ "$VTAG" == "GA" ] ; then
	VNAME="$VNUM-$VDATE"
else
	VNAME="$VNUM-$VTAG-$VDATE"
fi

echo "new NUMBER: $VNUM"
echo "new DATE: $VDATE"
echo "new TAG: $VTAG"
echo "new VERSION: $VNAME"

#alter version.properties
perl  -i'.orig' -p -e "s#^version\\.number\\s*=.*\$#version.number=$VNUM#" "$PWD/version.properties"
perl  -i'.orig' -p -e "s#^version\\.tag\\s*=.*\$#version.tag=$VTAG#" "$PWD/version.properties"
perl  -i'.orig' -p -e "s#^version\\.date\\s*=.*\$#version.date=$VDATE#" "$PWD/version.properties"
perl  -i'.orig' -p -e "s#^version\\.version\\s*=.*\$#version.version=$VNAME#" "$PWD/version.properties"

perl  -i'.orig' -p -e "s#^currentVersion\\s*=.*\$#currentVersion = $VNUM#" "$PWD"/gradle.properties

echo MODIFIED: "$(pwd)"/version.properties
