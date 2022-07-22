#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"

UPSTREAM_TAG="${UPSTREAM_TAG:-}"

DRY_RUN="${DRY_RUN:-true}"

set -euo pipefail

shopt -s globstar

main() {
    local COMMAND="${1}"
    shift

    case "${COMMAND}" in
        build) build "${@}" ;;
        build_pro) build_pro "${@}" ;;
        sign) sign "${@}" ;;
        test) test_packages "${@}" ;;
        test_pro) test_pro_packages "${@}" ;;
        publish) publish "${@}" ;;
        publish_pro) publish_pro "${@}" ;;
        publish_war) publish_war "${@}" ;;
        publish_war_pro) publish_war_pro "${@}" ;;
    esac
}

docker_login() {
    echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
}

build() {
    local RELEASE_NUM="1"
    echo  "---------Entering fetch artifcts---------"
    bash packaging/scripts/circle-build.sh fetch_artifacts
#    for file in packaging/*; do
#      echo "${file##*/}"
#    done
#    echo "--------WORKINGDIR--------"
#    echo "$PWD"
    (
        cd packaging/packaging
        ./gradlew \
            -PpackageRelease=$RELEASE_NUM \
            clean packageArtifacts
    )
}

build_pro() {
    local RELEASE_NUM="1"

    bash packaging/scripts/circle-build.sh fetch_artifacts
    (
        cd packaging/packaging
        ./gradlew \
            -PpackageRelease=$RELEASE_NUM \
            -PpackageInclude='.+(enterprise).+' \
            clean packageArtifacts
    )
}

sign() {
  bash packaging/scripts/sign-packages.sh
}

test_packages() {
  bash test/test-docker-install-deb.sh
  bash test/test-docker-install-rpm.sh
}

test_pro_packages() {
  docker_login
  test_packages
}

publish() {
  echo "publish function"
    (
        cd packaging/packaging
        for PACKAGE in deb rpm; do
            ./gradlew --info \
                -PpackagePrefix="rundeck-" \
                -PpackageType=$PACKAGE \
                -PpackageOrg=rundeck \
                -PpackageRevision=1 \
                publish
        done
    )
}

publish_pro() {
  echo "publish pro function"
#    (
#        cd packaging
#        for BUNDLE in enterprise; do
#            for PACKAGE in deb rpm; do
#                ./gradlew --info \
#                    -PpackageBundle=$BUNDLE \
#                    -PpackageType=$PACKAGE \
#                    -PpackageOrg=rundeckpro \
#                    -PpackageRevision=1 \
#                    publish
#            done
#        done
#    )
}

publish_war() {
  echo "publish war function"
    (
        cd packaging/packaging
        ./gradlew --info \
            -PpackageType=war \
            -PpackageOrg=rundeck \
            -PpackageRevision=1 \
            publishWar
    )
}

publish_pro_war() {
  echo 'publish pro war'
#    (
#        cd packaging
#        ./gradlew --info \
#            -PpackageType=war \
#            -PpackageGroup="com.rundeck.enterprise" \
#            -PpackageOrg=rundeckpro \
#            -PpackageRevision=1 \
#            publishWar
#    )
}

publish_to_s3() {
  echo 'publish to s3 function'
#    (
#        cd packaging
#        # This is a flag that doesn't take a value
#        S3_DRY_RUN="--dryrun"
#        if [[ "$DRY_RUN" != true ]] ; then
#            S3_DRY_RUN=""
#        fi
#
#        if [[ -n "${UPSTREAM_TAG}" ]] ; then
#            for PACKAGE in deb rpm; do
#                aws s3 sync "${S3_DRY_RUN}" --exclude=* --include=*.$PACKAGE packaging/build/distributions/ s3://download.rundeck.org/$PACKAGE/
#            done
#        fi
#    )
}

main "${@}"
