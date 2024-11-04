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
    LIB_DIR="${PACKAGING_DIR}/lib"
    ./gradlew ${GRADLE_BASE_OPTS} -PlibsDir=${LIB_DIR} -PpackageRelease=$RELEASE_NUM clean packageArtifacts
}
packaging_sign_retry(){
    N=${1:-5} # default 5
    total=$N
    while true; do
      N=$(( N - 1 ))
      if packaging_sign; then
        echo "Succeeded."
        exit 0
      fi
      [ $N -gt 0 ] || break
      echo "Retrying package signing in 10 seconds..."
      #clean up old files
      find "${PACKAGING_DIR}/packaging/build/distributions" -name '*.sig' -delete
      sleep 10
    done

    echo "FAILED after $total tries."
    exit 1
}
packaging_sign() {
    fetch_ci_shared_resources

    # Setup packaging env
    GPG_TTY=$(tty)
    export GPG_TTY
    bash "${PACKAGING_DIR}/packaging/scripts/sign-packages.sh"
}

packaging_test_packages() {
    bash "${PACKAGING_DIR}/test/test-docker-install-deb.sh"
    bash "${PACKAGING_DIR}/test/test-docker-install-rpm.sh"
}

packaging_publish() {
    cd "${PACKAGING_DIR}/packaging"
    LIB_DIR="${PACKAGING_DIR}/lib"
    for PACKAGE in deb rpm; do
        ./gradlew ${GRADLE_BASE_OPTS} --info \
            -PpackagePrefix="rundeck-" \
            -PpackageType=${PACKAGE} \
            -PlibsDir=${LIB_DIR} \
            -PpackageOrg=rundeck \
            -PpackageRevision=1 \
            publish
    done
}

packaging_publish_war() {
    cd "${PACKAGING_DIR}/packaging"
    LIB_DIR="${PACKAGING_DIR}/lib"
    ./gradlew ${GRADLE_BASE_OPTS} --info \
        -PpackageType=war \
        -PlibsDir=${LIB_DIR} \
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
        publishToSonatype closeAndReleaseSonatypeStagingRepository
}
