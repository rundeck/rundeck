#!/bin/bash
set -e

docker_login() {
    echo "${DOCKER_PASSWORD}" | docker login -u "${DOCKER_USERNAME}" --password-stdin
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

install_wizcli(){
  curl -Lo wizcli https://wizcli.app.wiz.io/latest/wizcli-linux-amd64
  chmod +x wizcli
  sudo cp ./wizcli /usr/local/bin
}

# Pull the image built on this build and adds a custom tag if provided as argument.
rundeck_pull_image() {
    docker_login
    local sourceTag="${DOCKER_CI_REPO}:${DOCKER_IMAGE_BUILD_TAG}"
    docker pull $sourceTag
    docker tag "${sourceTag}" "rundeck/testdeck"

    local customTag=${1:-}
    if [[ -n "${customTag}" ]]; then
        docker tag "${sourceTag}" "${customTag}"
    fi
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

wizcli_scan() {

    docker_login

    echo "==> Git Branch: ${RUNDECK_BRANCH}"
    echo "==> Git Tag: ${RUNDECK_TAG}"

    if [[ -n "${RUNDECK_TAG}" ]]; then
        export RUNDECK_IMAGE_TAG="${DOCKER_REPO}:${RUNDECK_TAG}"
    elif [[ "${RUNDECK_BRANCH}" == "main" ]]; then
        export RUNDECK_IMAGE_TAG="${DOCKER_REPO}:SNAPSHOT"
    else
        export RUNDECK_IMAGE_TAG="${DOCKER_CI_REPO}:${DOCKER_IMAGE_BUILD_TAG}"
    fi

    echo "==> Scan Image: ${RUNDECK_IMAGE_TAG}"

    docker pull "${RUNDECK_IMAGE_TAG}"

    #login to wizcli
    wizcli auth --id="${WIZCLI_ID}" --secret="${WIZCLI_SECRET}"

    # Scan showing only results that make policy fail.
    wizcli docker scan --image "${RUNDECK_IMAGE_TAG}" \
      --policy-hits-only \
      --format human \
      --show-vulnerability-details \
      --output "wizcli_scan_result.json,json,true" \
      --log wizcli.log

    wizexitcode=$?
    echo "WizExitCode: $wizexitcode"

    mkdir -p test-results/junit
    bash "${RUNDECK_CORE_DIR}/scripts/convert_wiz_junit.sh" wizcli_scan_result.json > test-results/junit/wizcli-junit.xml

    return $wizexitcode
}

openapi_tests() {
    # Extract openapi spec
    mkdir -p openapi
    find "${RUNDECK_WAR_DIR}" -name '*.war' -exec jar xvf \{\} WEB-INF/classes/META-INF/swagger/rundeck-api.yml \;
    mv WEB-INF/classes/META-INF/swagger/rundeck-api.yml openapi/

    # Redocly OpenAPI Linting
    npx -y @redocly/cli lint \
        openapi/rundeck-api.yml \
        --skip-rule=operation-4xx-response \
        --skip-rule=no-invalid-media-type-examples
}
