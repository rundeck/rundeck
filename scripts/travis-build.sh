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
        publish_oss) publish_oss ;;
    esac
}

build() {
    ./gradlew -Penvironment="${ENV}" -x check install ${RUNDECK_GRADLE_OPTS:-}
    groovy testbuild.groovy --buildType="${ENV}"
    # make ENV="${ENV}" rpm deb
}

buildFork() {
    ./gradlew -Penvironment="${ENV}" install
    groovy testbuild.groovy --buildType="${ENV}"
    # make ENV="${ENV}" rpm deb
}

buildDocker() {
    docker_login

    local CLEAN_TAG=$(echo $RUNDECK_BRANCH | tr '/' '-')
    local BRANCH_AS_TAG=branch${CLEAN_TAG}

    local ECR_BUILD_TAG=${ECR_REPO}:${ECR_IMAGE_PREFIX}-build-${RUNDECK_BUILD_NUMBER}
    local ECR_BRANCH_TAG=${ECR_REPO}:${BRANCH_AS_TAG}

    local CI_BRANCH_TAG=rundeck/ci:${CLEAN_TAG}

    ./gradlew officialBuild -Penvironment=${ENV} -PdockerTags=latest,SNAPSHOT

    docker tag rundeck/rundeck:latest $ECR_BUILD_TAG
    docker tag rundeck/rundeck:latest $ECR_BRANCH_TAG
    docker tag rundeck/rundeck:latest $CI_BRANCH_TAG

    docker push $ECR_BUILD_TAG
    docker push $ECR_BRANCH_TAG
    docker push $CI_BRANCH_TAG

    if [[ "${RUNDECK_MASTER_BUILD}" = true && -z "${RUNDECK_TAG}" ]] ; then
        ./gradlew officialPush -PdockerTags=SNAPSHOT
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

publish_oss() {
    ./gradlew \
        -PsonatypeUsername="${SONATYPE_USERNAME}" \
        -PsonatypePassword="${SONATYPE_PASSWORD}" \
        uploadExisting --info
}

main "${@}"
