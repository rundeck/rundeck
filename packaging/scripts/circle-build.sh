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
        create_packages) create_packages "${@}" ;;
        sign) sign "${@}" ;;
        test) test_packages "${@}" ;;
        publish) publish "${@}" ;;
        publish_war) publish_war "${@}" ;;
    esac
}

docker_login() {
    echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
}

create_packages() {
    local RELEASE_NUM="1"
    bash packaging/packaging/scripts/circle-build.sh fetch_artifacts
    (
        cd packaging/packaging
        ./gradlew \
            -PpackageRelease=$RELEASE_NUM \
            clean packageArtifacts
    )
}

fetch_ci_resources() {
    aws s3 sync --delete "${S3_CI_RESOURCES}" ~/.gnupg
}

sign() {
  fetch_ci_resources
  bash packaging/packaging/scripts/sign-packages.sh
}

test_packages() {
  bash packaging/test/test-docker-install-deb.sh
  bash packaging/test/test-docker-install-rpm.sh
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

main "${@}"
