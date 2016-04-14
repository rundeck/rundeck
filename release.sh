#!/bin/bash
#/ Update version to release version, create a release tag and, then update to new snapshot version.
#/ usage: [--commit]
#/   --commit: commit changes. otherwise a DRYRUN is performed

set -euo pipefail
IFS=$'\n\t'
readonly ARGS=("$@")
DRYRUN=1
. rd_versions.sh

die(){
    echo >&2 "$@" ; exit 2
}

usage() {
      grep '^#/' <"$0" | cut -c4- # prints the #/ lines above as usage info
}
do_dryrun(){
    local FARGS=("$@")
    if [ $DRYRUN -eq 0 ]; then
        "${FARGS[@]}"
    else
        echo "DRYRUN: " "${FARGS[@]}"
    fi
}

rd_check_current_version_is_not_GA(){
    local VERS=("$@")
    echo "Current version: ${VERS[0]}-${VERS[1]}-${VERS[2]}"

    if [ "${VERS[2]}" == "GA" ] ; then
        echo "GA Release already, done."
        exit 0
    fi
}
commit_changes(){
    local MESSAGE=$1
    echo "Commit changes.."
    do_dryrun git add -u .
    do_dryrun git commit -m "$MESSAGE"
}
commit_tag(){
    local FARGS=("$@")
    local MESSAGE=${FARGS[1]}
    local TAG=${FARGS[0]}
    echo "Create tag $TAG.."
    do_dryrun git tag -a $TAG -m "$MESSAGE"
}
check_args(){
    if [ ${#ARGS[@]} -gt 1 ] ; then
        usage
        exit 2
    fi
    if [ ${#ARGS[@]} -gt 0 ] && [ "${ARGS[0]}" == "--help" -o "${ARGS[0]}" == "-h" -o "${ARGS[0]}" == "-?"  ] ; then
        usage
        exit 2
    fi
    if [ ${#ARGS[@]} -gt 0 ] && [ "${ARGS[0]}" == "--commit" ] ; then
        DRYRUN=0
    fi
}
check_release_notes(){
    local PATTERN=$1
    set +e
    local output=$( head -n 1 < RELEASE.md | grep "$PATTERN" )
    local result=$?
    set -e

    if [ $result -ne 0 ] ; then
        die "ERROR: RELEASE.md has not been updated, please add release notes."
    fi
}
check_git_is_clean(){
    set +e
    # find any git file with status other than ?? (untracked)
    git status --porcelain | grep '^[^?][^?]'
    local status=$?
    set -e
    if [ $status -eq 0 ] ; then
        die "ERROR: Git has modified files, please stash/commit any changes before updating version."
    fi
}
#/ Run the makefile to copy RELEASE.md into the documentation source, and git add the changes.
generate_release_notes_documentation(){
    local NEW_VERS=$1
    local DDATE=$(date "+%Y-%m-%d")
    sed "s#Date: ....-..-..#Date: $DDATE#" < RELEASE.md > RELEASE.md.new
    mv RELEASE.md.new RELEASE.md
    make -C docs notes
    git add RELEASE.md
    git add CHANGELOG.md
    git add docs/en/history/version-${NEW_VERS}.md
    git add docs/en/history/toc.conf
    git add docs/en/history/changelog.md
    local out=$( git status --porcelain | grep '^M  docs/en/history/toc.conf') || die "docs/en/history/toc.conf was not modified"
    out=$( git status --porcelain | grep "^A  docs/en/history/version-${NEW_VERS}.md") || die "docs/en/history/version-${NEW_VERS}.md was not added"
    out=$( git status --porcelain | grep "^M  docs/en/history/changelog.md") || die "docs/en/history/changelog.md was not modified"
    out=$( git status --porcelain | grep "^M  CHANGELOG.md") || die "CHANGELOG.md was not modified"
}


main() {
    check_args

    local -a VERS=( $( rd_get_version ) )

    rd_check_current_version_is_not_GA  "${VERS[@]}"

    check_git_is_clean

    local -a NEW_VERS=( $( rd_make_release_version "${VERS[@]}" ) )

    check_release_notes "Release ${NEW_VERS[0]}"

    rd_set_version "${NEW_VERS[@]}"

    commit_changes "Update version to ${NEW_VERS[0]}-${NEW_VERS[1]}-${NEW_VERS[2]}"

    generate_release_notes_documentation "${NEW_VERS[@]}"

    commit_changes "Update release documentation for ${NEW_VERS[0]}-${NEW_VERS[1]}-${NEW_VERS[2]}"

    commit_tag "v${NEW_VERS[0]}" "Release version ${NEW_VERS[0]}-${NEW_VERS[1]}-${NEW_VERS[2]}"

    local -a NEXT_VERS=( $( rd_make_next_snapshot "${VERS[@]}" ) )

    rd_set_version "${NEXT_VERS[@]}"

    commit_changes "Update version to ${NEXT_VERS[0]}-${NEXT_VERS[1]}-${NEXT_VERS[2]}"

}
main


