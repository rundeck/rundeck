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
    docker_login

    local BRANCH_AS_TAG=branch-$(echo $RUNDECK_BRANCH | tr '/' '-')

    local ECR_BUILD_TAG=${ECR_REPO}:build-${RUNDECK_BUILD_NUMBER}
    local ECR_BRANCH_TAG=${ECR_REPO}:${BRANCH_AS_TAG}

    ./gradlew officialBuild -PdockerTags=branch-$BRANCH_AS_TAG,latest,SNAPSHOT

    docker tag rundeck/rundeck:latest $ECR_BUILD_TAG
    docker tag rundeck/rundeck:latest $ECR_BRANCH_TAG

    docker push $ECR_BUILD_TAG
    docker push $ECR_BRANCH_TAG

    if [[ "${RUNDECK_MASTER_BUILD}" = true ]] ; then
        ./gradlew officialPush -PdockerTags=SNAPSHOT
    else
        ./gradlew officialPush -PdockerTags=$BRANCH_AS_TAG
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
