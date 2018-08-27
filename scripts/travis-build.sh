#!/bin/bash

set -euo pipefail

main() {
    ENV=development

    if [[ ! -z "${RUNDECK_TAG:-}" ]] ; then
        ENV=release
    fi

    case "${1}" in
        build) build ;;
        buildFork) buildFork ;;
        buildDocker) buildDocker ;;
        publish) publish ;;
    esac
}

build() {
    ./gradlew -Penvironment="${ENV}" -x check install
    groovy testbuild.groovy --buildType="${ENV}"
    make ENV="${ENV}" rpm deb
}

buildFork() {
    ./gradlew -Penvironment="${ENV}" install
    groovy testbuild.groovy --buildType="${ENV}"
    make ENV="${ENV}" rpm deb
}

buildDocker() {
    if [[ "${RUNDECK_MASTER_BUILD}" = true ]] ; then
        docker_login && ./gradlew officialPush
    else
        echo "Skipping docker build for non-master build."
    fi
}

publish() {
    ./gradlew \
        -Penvironment="${ENV}" \
        -PdryRun="${DRY_RUN}" \
        -PbintrayUseExisting="true" \
        -PbintrayUser="${BINTRAY_USER}" \
        -PbintrayApiKey="${BINTRAY_API_KEY}" \
        -PsigningPassword="${RUNDECK_SIGNING_PASSWORD}" \
        -PsonatypeUsername="${SONATYPE_USERNAME}" \
        -PsonatypePassword="${SONATYPE_PASSWORD}" \
        bintrayUpload --info
}

main "${@}"
