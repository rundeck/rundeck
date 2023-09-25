#!/bin/bash
set -e

docker_login() {
    echo "${DOCKER_PASSWORD}" | docker login -u "${DOCKER_USERNAME}" --password-stdin
}

docker_ecr_login() {
    docker_login
    aws ecr get-login-password --region us-west-2 | docker login --username AWS --password-stdin "${ECR_REGISTRY}"
}

install_twistcli() {
    curl -sSL -u "${TWISTLOCK_USER}:${TWISTLOCK_PASSWORD}" "${TWISTLOCK_CONSOLE_URL}/api/v1/util/twistcli" --output ./twistcli
    chmod +x twistcli
}

install_package_cloud() {
    sudo gem install package_cloud

    # Install packagecloud token.
    echo "{\"url\":\"https://packagecloud.io\",\"token\":\"${PKGCLD_WRITE_TOKEN}\"}" >~/.packagecloud
}

collect_gradle_tests() {
    mkdir -p ~/test-results/junit/
    find . -type f -regex ".*/build/test-results/.*xml" -exec cp {} ~/test-results/junit/ \;
}

collect_build_artifacts() {
    shopt -s globstar
    mkdir -p "${WORKDIR}/artifacts"
    cp -pvr --parents core/build/libs "${WORKDIR}/artifacts"
    cp -pvr --parents rundeckapp/build/libs "${WORKDIR}/artifacts"
    cp -pvr --parents rundeck-storage/**/build/libs "${WORKDIR}/artifacts"
    cp -pvr --parents rundeck-authz/**/build/libs "${WORKDIR}/artifacts"
}

# Copies the built war to the location specified as parameter
copy_rundeck_war() {
    local destFile=${1}
    if [[ -z "${destFile}" ]]; then
        echo "No destination path specified"
        return 1
    fi

    local warFile
    warFile=$(find "${RUNDECK_WAR_DIR}" -name '*[A-Z0-9].war' -print -quit)

    if [[ -z "${warFile}" || ! -f "${warFile}" ]]; then
        echo "Could not find war file: ${warFile}"
        return 1
    fi

    cp -pv "${warFile}" "${destFile}"
}

fetch_ci_shared_resources() {
    # Get ci resources.
    aws s3 sync --delete "${S3_CI_SHARED_RESOURCES}" ci-resources

    echo "Retrieved resources from AWS!! "
    ls -la ci-resources

    # install gpg keys.
    mkdir -p "${HOME}/.gnupg"
    cp -pv ci-resources/* "${HOME}/.gnupg/"
    chmod -R 700 "${HOME}/.gnupg"
}

twistlock_scan() {

    docker_login

    echo "==> Git Branch: ${RUNDECK_BRANCH}"
    echo "==> Git Tag: ${RUNDECK_TAG}"

    if [[ -n "${RUNDECK_TAG}" ]]; then
        #If the build is triggered by a git Tag
        export RUNDECK_IMAGE_TAG="${DOCKER_REPO}:${RUNDECK_TAG}"
    elif [[ "${RUNDECK_BRANCH}" == "main" ]]; then
        export RUNDECK_IMAGE_TAG="${DOCKER_REPO}:SNAPSHOT"
    else
        export RUNDECK_IMAGE_TAG="${DOCKER_CI_REPO}:${RUNDECK_BRANCH_CLEAN}"
    fi

    echo "==> Scan Image: ${RUNDECK_IMAGE_TAG}"

    docker pull "${RUNDECK_IMAGE_TAG}"

    # We run twistcli as root so it can start containers within its circleci container without issues.
    sudo ./twistcli images scan --details \
        -address "${TWISTLOCK_CONSOLE_URL}" \
        -u "${TWISTLOCK_USER}" -p "${TWISTLOCK_PASSWORD}" \
        --output-file twistlock_scan_result.json "${RUNDECK_IMAGE_TAG}"

    # Fix permissions of created file.
    sudo chown "${CURRENT_USER}" twistlock_scan_result.json

    #Translate report to junit format.
    mkdir -p test-results/junit
    bash "${RUNDECK_CORE_DIR}/scripts/convert_tl_junit.sh" twistlock_scan_result.json >test-results/junit/twistlock-junit.xml

    #report severity filter to extract incidents count, default: high and critical
    local reportSeverityFilter='.results[0].vulnerabilityDistribution.high + .results[0].vulnerabilityDistribution.critical'
    local incidents=$(cat twistlock_scan_result.json | jq "$reportSeverityFilter")

    if [[ $incidents -gt 0 ]]; then
        echo "==> Security Alert: found vulnerabilities, $incidents of them must be mitigated before release. Please refer to the above report for detail."
    fi

    return $incidents
}

openapi_tests() {
    # Extract openapi spec
    mkdir -p openapi
    find "${RUNDECK_WAR_DIR}" -name '*.war' -exec jar xvf \{\} WEB-INF/classes/META-INF/swagger/rundeck-api.yml \;
    mv WEB-INF/classes/META-INF/swagger/rundeck-api.yml openapi/

    # Redocly OpenAPI Linting
    npm install @redocly/cli -g
    redocly lint \
        openapi/rundeck-api.yml \
        --skip-rule=operation-4xx-response \
        --skip-rule=no-invalid-media-type-examples
}
