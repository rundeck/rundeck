#!/bin/bash

set -euo pipefail

main() {
    ENV=development

    if [[ ! -z "${RUNDECK_TAG:-}" ]] ; then
        ENV=release
    fi

    case "${1}" in
        build) build ;;
        publish) publish ;;
    esac
}

build() {
    ./gradlew -Penvironment="${ENV}" -x check install
    groovy testbuild.groovy --buildType="${ENV}"
    make ENV="${ENV}" rpm deb
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
