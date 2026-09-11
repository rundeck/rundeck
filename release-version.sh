#!/bin/bash
#
# release-version.sh - shared version-string / tag-name helpers.
#
# Sourced (never executed) by setversion.sh and release-rc.sh so both scripts
# validate and format MAJOR.MINOR.PATCH versions the same way instead of each
# carrying its own copy of the regex/formatting.

# validate_version_format <version>
# Checks <version> is MAJOR.MINOR.PATCH (e.g. 5.19.1). Prints an error and returns
# 1 otherwise; the caller decides the exit code.
function validate_version_format {
    local version="$1"
    if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        echo "Error: Version ($version) must be in MAJOR.MINOR.PATCH format (e.g., 5.19.1)"
        return 1
    fi
}

# version_major_minor <version>
# Echoes the MAJOR.MINOR portion of a MAJOR.MINOR.PATCH version string.
function version_major_minor {
    local major minor _patch
    IFS='.' read -r major minor _patch <<< "$1"
    echo "$major.$minor"
}

# parse_rc_number <tag>
# If <tag> matches rc<N>, echoes N and returns 0. Otherwise returns 1 with no output.
function parse_rc_number {
    local tag="$1"
    if [[ "$tag" =~ ^rc([0-9]+)$ ]]; then
        echo "${BASH_REMATCH[1]}"
        return 0
    fi
    return 1
}

# version_tag_name <version> <suffix>
# Builds the release tag name for a non-GA release: v<version>-<suffix>
# (e.g. version_tag_name 7.4.10 rc2 -> v7.4.10-rc2).
function version_tag_name {
    echo "v$1-$2"
}
