#!/bin/bash
set -e

packaging_setup() {
    # Setup submodule repository
#    sudo sed -i 's/git@github.com:/https:\/\/github.com\//' .gitmodules
    git submodule update --init --recursive --remote

}

packaging_create_packages() {
    local RELEASE_NUM="1"

    mkdir -p "${PACKAGING_DIR}/packaging/artifacts/"
    copy_rundeck_war "${PACKAGING_DIR}/packaging/artifacts/"

    cd "${PACKAGING_DIR}/packaging"
    ./gradlew ${GRADLE_BASE_OPTS} -PpackageRelease=$RELEASE_NUM clean packageArtifacts
}

packaging_sign() {
    fetch_ci_shared_resources

    # Setup packaging env
    GPG_TTY=$(tty)
    export GPG_TTY
    bash "${PACKAGING_DIR}/packaging/scripts/sign-packages.sh"
}

packaging_test_packages() {
    docker_login
    bash "${PACKAGING_DIR}/test/test-docker-install-deb.sh"
    bash "${PACKAGING_DIR}/test/test-docker-install-rpm.sh"
}

packaging_publish() {
    cd "${PACKAGING_DIR}/packaging"
    for PACKAGE in deb rpm; do
        ./gradlew ${GRADLE_BASE_OPTS} --info \
            -PpackagePrefix="rundeck-" \
            -PpackageType=${PACKAGE} \
            -PpackageOrg=rundeck \
            -PpackageRevision=1 \
            publish
    done
}

packaging_publish_war() {
    cd "${PACKAGING_DIR}/packaging"
    ./gradlew ${GRADLE_BASE_OPTS} --info \
        -PpackageType=war \
        -PpackageOrg=rundeck \
        -PpackageRevision=1 \
        publishWar
}

packaging_publish_maven() {

    DRY_RUN=${DRY_RUN:-false}

    fetch_ci_shared_resources

    ./gradlew ${GRADLE_BASE_OPTS} \
        -Penvironment="${ENV}" \
        -PdryRun="${DRY_RUN}" \
        -Psigning.secretKeyRingFile="$(realpath "${HOME}/.gnupg/secring.gpg")" \
        -Psigning.password="${RUNDECK_SIGNING_PASSWORD}" \
        -Psigning.keyId="${RUNDECK_SIGNING_KEYID}" \
        -PsigningPassword="${RUNDECK_SIGNING_PASSWORD}" \
        -PsonatypeUsername="${SONATYPE_USERNAME}" \
        -PsonatypePassword="${SONATYPE_PASSWORD}" \
        publishToSonatype closeSonatypeStagingRepository
#        publishToSonatype closeAndReleaseSonatypeStagingRepository
}
