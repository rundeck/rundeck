#!/bin/bash

CUR_VERSION="$(grep version.number= "$PWD/version.properties" | cut -d= -f 2)"
CUR_TAG="$(grep version.tag= "$PWD/version.properties" | cut -d= -f 2)"

echo "current NUMBER: $CUR_VERSION"
echo "current TAG: $CUR_TAG"

function usage {
    echo "Usage:"
    echo "  setversion.sh <version> [GA|rc#|alpha#]           - Update version in version.properties"
    echo "  setversion.sh --bump-minor                            - Bump minor version number"
    echo "  setversion.sh --tag <version> [GA|rc#|alpha#]     - Create git tag for release directly on current branch"
    echo "  setversion.sh --create-release-branch <version>       - Create release branch for patch releases"
    exit 2
}

if [ -z "$1" ] ; then
    usage
fi

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

    # Verify we're on main or a release branch
    CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
    if [[ "$CURRENT_BRANCH" != "main" && ! "$CURRENT_BRANCH" =~ ^release/.* ]]; then
        echo "Error: Must be on 'main' branch or a release branch to create release tags"
        exit 4
    fi

    # Create the appropriate tag
    if [ "$VTAG" == "GA" ]; then
        TAG_NAME="v$VNUM"
    elif [[ "$VTAG" =~ ^[a-z]+[0-9]+$ ]]; then
        TAG_NAME="v$VNUM-$VTAG"
    else
      echo "Error: Invalid tag format '$VTAG'. Expected 'GA' or to match [a-z]+[0-9]+ (e.g., rc1, rc2, alpha3)."
      exit 5
    fi

    echo "Creating tag: $TAG_NAME"
    git tag -a "$TAG_NAME" -m "Release $VNUM $VTAG"
    echo "Tag created. Use 'git push origin $TAG_NAME' to push the tag to remote."
    exit 0

# Create a release branch for patch releases
elif [ "$1" == "--create-release-branch" ]; then
    shift
    if [ -z "$1" ] ; then
        echo "Error: Version required"
        usage
    fi
    VNUM="$1"
    # Extract major.minor and patch
    IFS='.' read -r MAJOR MINOR PATCH <<< "$VNUM"
    if [ -z "$MAJOR" ] || [ -z "$MINOR" ] || [ -z "$PATCH" ]; then
        echo "Error: Version ($VNUM) must be in MAJOR.MINOR.PATCH format (e.g., 5.19.1)"
        exit 3
    fi

    # Check if this is a patch version (patch > 0)
    if [ "$PATCH" -eq 0 ]; then
        echo "Warning: Creating a patch release branch typically requires a patch version > 0 (e.g., 5.19.1, not 5.19.0)"
        read -p "Do you want to continue with $VNUM? (y/n): " confirm
        if [[ "$confirm" != [yY] ]]; then
            echo "Operation cancelled"
            exit 0
        fi
    fi

    BASE_VERSION="$MAJOR.$MINOR.0"
    BRANCH_NAME="release/$MAJOR.$MINOR.x"
    GA_TAG="v$BASE_VERSION"

    # Check if the GA tag exists
    if ! git rev-parse --verify "$GA_TAG" >/dev/null 2>&1; then
        echo "Error: GA tag $GA_TAG not found. The release branch should be created from the GA tag."
        echo "Verify that the GA release has been tagged correctly first."
        exit 6
    fi

    # Check if branch already exists locally or remotely
    if git rev-parse --verify "$BRANCH_NAME" >/dev/null 2>&1 ||
       git ls-remote --heads origin "$BRANCH_NAME" | grep -q "$BRANCH_NAME"; then
        echo "Warning: Branch $BRANCH_NAME already exists locally or remotely."
        read -p "Do you want to use the existing branch? (y/n): " use_existing
        if [[ "$use_existing" == [yY] ]]; then
            echo "Checking out existing branch: $BRANCH_NAME"
            git checkout "$BRANCH_NAME" || exit 7
            git pull origin "$BRANCH_NAME" || echo "Warning: Failed to pull latest changes from remote"
        else
            echo "Operation cancelled"
            exit 0
        fi
    else
        # Create new branch from the GA tag
        echo "Creating release branch $BRANCH_NAME from tag $GA_TAG"
        git checkout -b "$BRANCH_NAME" "$GA_TAG" || exit 7
    fi

    # Update version in version.properties to patch-SNAPSHOT
    VDATE="$(date +%Y%m%d)"
    SNAPSHOT_VERSION="$VNUM-SNAPSHOT-$VDATE"
    echo "Setting version to $SNAPSHOT_VERSION in version.properties"

    perl -i'.orig' -p -e "s#^version\\.number\\s*=.*\$#version.number=$VNUM#" "$PWD/version.properties"
    perl -i'.orig' -p -e "s#^version\\.tag\\s*=.*\$#version.tag=SNAPSHOT#" "$PWD/version.properties"
    perl -i'.orig' -p -e "s#^version\\.date\\s*=.*\$#version.date=$VDATE#" "$PWD/version.properties"
    perl -i'.orig' -p -e "s#^version\\.version\\s*=.*\$#version.version=$SNAPSHOT_VERSION#" "$PWD/version.properties"

    echo "Modified: $(pwd)/version.properties"

    # Commit the version change
    git add version.properties
    git status | grep "version.properties" --quiet
    if [ $? -eq 0 ]; then
        echo "Committing version change"
        git commit -m "Set version to $SNAPSHOT_VERSION for patch release"
        echo "Changes committed"
    else
        echo "No changes to commit in version.properties"
    fi

    echo ""
    echo "Release branch $BRANCH_NAME created successfully."
    echo "Use 'git push origin $BRANCH_NAME' to push to remote."
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
