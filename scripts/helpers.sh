#!/bin/bah

# Breaks up git version tag into its constituent parts
# Outputs "VERSION_NUM RELEASE_TAG RELEASE_TAG_NUMBER|0"

set -e

tag_parts() {
    local TAG=${1:-''}

    if [[ $TAG =~ ^v(([0-9]+\.?)+)-?([a-z0-9]+)?-?([0-9]+)? ]] ; then
        local RUNDECK_VERSION=${BASH_REMATCH[1]}
        local RUNDECK_RELEASE_TAG=${BASH_REMATCH[3]:-GA}
        local RUNDECK_RELEASE_TAG_NUMBER=${BASH_REMATCH[4]:-0}
        echo -n "${RUNDECK_VERSION} ${RUNDECK_RELEASE_TAG} ${RUNDECK_RELEASE_TAG_NUMBER}"
        return 0
    elif [[ -z $TAG ]] ; then
        return 0
    else
        echo "${TAG} is not a valid version tag!"
        return 1
    fi
}

release_tag_repo() {
    local TYPE=${1:?'Repo type must be (deb|rpm|maven)'}
    local RELEASE_TAG=${2:?'Release type must be (SNAPSHOT|GA|*)'}

    if [[ "${TYPE}" == 'deb' ]]; then
        case $RELEASE_TAG in
            'SNAPSHOT')
                echo -n 'ci-snapshot-deb'
                ;;
            'GA')
                echo -n 'rundeck-deb'
                ;;
            *)
                echo -n 'beta-deb'
                ;;
        esac
    elif [[ "${TYPE}" == 'rpm' ]] ; then
        case $RELEASE_TAG in
            'SNAPSHOT')
                echo -n 'ci-snapshot-rpm'
                ;;
            'GA')
                echo -n 'rundeck-rpm'
                ;;
            *)
                echo -n 'beta-rpm'
                ;;
        esac
    elif [[ "${TYPE}" == 'maven' ]] ; then
        case $RELEASE_TAG in
            'SNAPSHOT')
                echo -n 'ci-snapshot-maven'
                ;;
            'GA')
                echo -n 'rundeck-maven'
                ;;
            *)
                echo -n 'beta-maven'
                ;;
        esac
    fi
}

if [[ "${0}" != "${BASH_SOURCE}" ]] ; then
    true
else
    COMMAND=${1:-''}
    shift
    case $COMMAND in
        'tag_parts')
            tag_parts "${@}"
            ;;
        'release_tag_repo')
            release_tag_repo "${@}"
            ;;
        *)
            echo 'Unkown command'
            ;;
    esac
fi