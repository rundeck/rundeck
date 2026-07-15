#!/bin/bash

CUR_VERSION="$(grep version.number= "$PWD/version.properties" | cut -d= -f 2)"
CUR_TAG="$(grep version.tag= "$PWD/version.properties" | cut -d= -f 2)"

echo "current NUMBER: $CUR_VERSION"
echo "current TAG: $CUR_TAG"

function usage {
    echo "Usage:"
    echo "  setversion.sh <version> [GA|rc#|alpha#]                                              - Update version in version.properties"
    echo "  setversion.sh --bump-minor                                                           - Bump minor version number"
    echo "  setversion.sh --tag <version> [GA|rc#|alpha#] [--push] [--dry-run] [--debug]        - Create git tag; checks out release branch if one exists for the version"
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

# Git wrapper function for dry-run support
function git() {
    if [ "$DRY_RUN" = true ]; then
        case "$1" in
            rev-parse|show-ref|diff|log|status|branch|ls-remote|symbolic-ref)
                command git "$@"
                ;;
            checkout|tag|push|commit|add)
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

    # Create the appropriate tag
    if [ "$VTAG" == "GA" ]; then
        TAG_NAME="v$VNUM"
    elif [[ "$VTAG" =~ ^[a-z]+[0-9]+$ ]]; then
        TAG_NAME="v$VNUM-$VTAG"
    else
      echo "Error: Invalid tag format '$VTAG'. Expected 'GA' or to match [a-z]+[0-9]+ (e.g., rc1, rc2, alpha3)."
      exit 5
    fi

    IFS='.' read -r MAJOR MINOR PATCH <<< "$VNUM"
    if [ -z "$MAJOR" ] || [ -z "$MINOR" ] || [ -z "$PATCH" ]; then
        echo "Error: Version ($VNUM) must be in MAJOR.MINOR.PATCH format"
        exit 3
    fi

    RELEASE_BRANCH="release/$MAJOR.$MINOR.x"
    if git rev-parse --verify "$RELEASE_BRANCH" >/dev/null 2>&1 ||
       git ls-remote --heads origin "$RELEASE_BRANCH" | grep -q "refs/heads/${RELEASE_BRANCH}$"; then
        RELEASE_BRANCH_EXISTS=true
    else
        RELEASE_BRANCH_EXISTS=false
    fi

    if [ "$PATCH" -ne 0 ] && [ "$RELEASE_BRANCH_EXISTS" = false ]; then
        echo "Error: Release branch $RELEASE_BRANCH does not exist."
        echo "Create it first with: setversion.sh --create-release-branch $VNUM"
        exit 9
    fi

    if [ "$RELEASE_BRANCH_EXISTS" = true ]; then
        # Use the release branch (required for patch > 0; preferred for patch == 0 when a branch was cut early)
        CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
        if [ "$CURRENT_BRANCH" != "$RELEASE_BRANCH" ]; then
            echo "Checking out release branch: $RELEASE_BRANCH"
            git checkout "$RELEASE_BRANCH" || exit 10
        fi
    else
        echo "No release branch $RELEASE_BRANCH found, tagging from current HEAD"
    fi

    echo "Creating tag: $TAG_NAME"

    # Verify we're on main or a release branch (skip in dry-run)
    if [ "$DRY_RUN" = false ]; then
        CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
        if [[ "$CURRENT_BRANCH" != "main" && ! "$CURRENT_BRANCH" =~ ^release/.* ]]; then
            echo "Error: Must be on 'main' branch or a release branch to create release tags"
            exit 4
        fi
    fi

    git tag -a "$TAG_NAME" -m "Release $VNUM $VTAG"
    echo "Tag created: $TAG_NAME"

    if [ "$PUSH_TO_ORIGIN" = true ]; then
        echo "Pushing tag to remote..."
        git push origin "$TAG_NAME"
        echo "Tag pushed to remote."
    else
        echo "Use 'git push origin $TAG_NAME' to push the tag to remote."
    fi
    exit 0

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
