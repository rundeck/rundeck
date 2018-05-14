#!/bin/bash
#/ Update version to release version, create a release tag and, then update to new snapshot version.
#/ usage: [--dryrun|--commit]
#/   --dryrun: don't commit changes
#/   --commit: commit changes

set -euo pipefail
IFS=$'\n\t'
readonly ARGS=("$@")
DRYRUN=1
SIGN=0
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
    if [ "$SIGN" == "1" ] ; then
        do_dryrun git tag -s $TAG -m "$MESSAGE"
    else
        do_dryrun git tag -a $TAG -m "$MESSAGE"
    fi
}
check_args(){
    if [ ${#ARGS[@]} -lt 1 ] ; then
        usage
        exit 2
    fi
    if [ ${#ARGS[@]} -gt 0 ] && [ "${ARGS[0]}" == "--help" -o "${ARGS[0]}" == "-h" -o "${ARGS[0]}" == "-?"  ] ; then
        usage
        exit 2
    fi
    if [ ${#ARGS[@]} -gt 0 ] && [ "${ARGS[0]}" == "--commit" ] ; then
        DRYRUN=0
    elif [ ${#ARGS[@]} -gt 0 ] && [ "${ARGS[0]}" == "--dryrun" ] ; then
        DRYRUN=1
    else
        usage
        exit 2
    fi
    if [ ${#ARGS[@]} -gt 1 ] && [ "${ARGS[1]}" == "--sign" ] ; then
        SIGN=1
    fi
}
check_release_notes(){
    local PATTERN=$1
    set +e
    local result=$( head -n 1 < RELEASE.md | grep -c "$PATTERN" )
    set -e

    if [ $result != "1" ] ; then
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
generate_release_name(){
    local vers=$1
    local osascript=$(which osascript)
    local TEMPL='<span style="color: $REL_COLOR"><span class="glyphicon glyphicon-$REL_ICON"></span> "$REL_TEXT"</span>'
    if [ -n "$osascript" ] ; then
        # run javascript file with osascript (mac)
        local vars=$(cat rundeckapp/grails-app/assets/javascripts/version.js  releaseversion.js  | osascript -l JavaScript - $vers )
        eval $vars
        echo $TEMPL | sed "s#\$REL_COLOR#$REL_COLOR#" \
            | sed "s#\$REL_ICON#$REL_ICON#" \
            | sed "s#\$REL_TEXT#$REL_TEXT#"
    fi
}
#/ Update date/name for release notes in RELEASE.md and git add the changes.
generate_release_notes_documentation(){
    local NEW_VERS=$1
    local DDATE=$(date "+%Y-%m-%d")
    sed "s#Date: ....-..-..#Date: $DDATE#" < RELEASE.md > RELEASE.md.new
    mv RELEASE.md.new RELEASE.md
    local RELNAME=$(generate_release_name $NEW_VERS)
    if [ -z "$RELNAME" ] ; then
        die "Failed to generate release name"
    fi
    sed "s#Name: <span.*/span>#Name: $RELNAME#" < RELEASE.md > RELEASE.md.new
    mv RELEASE.md.new RELEASE.md

    git add RELEASE.md
    git add CHANGELOG.md
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


