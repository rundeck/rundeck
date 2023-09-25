#!/bin/bash
set -e

rundeck_war_build() {
    echo "== Versions =="
    java -version
    echo "NPM=$(npm -version)"
    echo "Node=$(node --version)"
    echo "Groovy=$(groovy --version)"
    echo "== Launch SPA build =="
    # It appears npm has issues with parallel builds, so we build grails-spa first without parallelism.
    ./gradlew -Penvironment="${ENV}" ${GRADLE_BASE_OPTS} -x check --no-parallel --max-workers 1 \
        rundeckapp:grails-spa:build runNpmBuild

    echo "== Launch War build =="
    ./gradlew -Penvironment="${ENV}" ${GRADLE_BUILD_OPTS} publishToMavenLocal build -x check
}

rundeck_gradle_tests() {
    ./gradlew -Penvironment="${ENV}" ${GRADLE_BUILD_OPTS} check
}

rundeck_docker_build() {
    #Build image
    ./gradlew ${GRADLE_BASE_OPTS} officialBuild -Penvironment=${ENV} -PdockerRepository=${DOCKER_REPO} -PdockerTags=latest,SNAPSHOT

    docker tag "${DOCKER_REPO}:latest" "${ECR_REPO}:${ECR_IMAGE_TAG}"

    # CircleCI tag builds do not have a branch set
    if [[ -n "${RUNDECK_BRANCH_CLEAN}" ]]; then
        local CI_BRANCH_TAG=${DOCKER_CI_REPO}:${RUNDECK_BRANCH_CLEAN}
        docker tag "${DOCKER_REPO}:latest" "$CI_BRANCH_TAG"
    fi

}

rundeck_docker_push() {
    docker_ecr_login

    docker push "${ECR_REPO}:${ECR_IMAGE_TAG}"

    # CircleCI tag builds do not have a branch set
    if [[ -n "${RUNDECK_BRANCH_CLEAN}" ]]; then
        local CI_BRANCH_TAG=${DOCKER_CI_REPO}:${RUNDECK_BRANCH_CLEAN}
        docker push "$CI_BRANCH_TAG"
    fi

    if [[ "${RUNDECK_MAIN_BUILD}" = true && -z "${RUNDECK_TAG}" ]]; then
        ./gradlew ${GRADLE_BASE_OPTS} officialPush -PdockerRepository=${DOCKER_REPO} -PdockerTags=SNAPSHOT
    fi
}

rundeck_docker_publish() {
    docker_login
    ./gradlew ${GRADLE_BASE_OPTS} -Penvironment="${ENV}" -PdockerRepository=${DOCKER_REPO} docker:officialPush
}

rundeck_verify_build() {
    groovy testbuild.groovy --buildType="${ENV}" -debug
}
