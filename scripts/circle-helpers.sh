#!/usr/bin/env bash
# Exports environment variables and functions for use in CI environments
#
# Exported variables
# ==================
# RUNDECK_BUILD_NUMBER = build number from RUNDECK_BUILD_NUMBER
# RUNDECK_TAG = Git tag used for this build and extracted from CI_TAG
# RUNDECK_RELEASE_TAG = (SNAPSHOT|GA|other) extracted from CI_TAG
# RUNDECK_RELEASE_VERSION = version number extracted from CI_TAG
# RUNDECK_PREV_RELEASE_VERSION = same as above extracted from second oldest tag in git history
# RUNDECK_PREV_RELEASE_VERSION = same as above extracted from second oldest tag in git history
# RUNDECK_MAIN_BUILD = (true|false) resolves TCI funkyness to determine if this is a true main build

set -eo pipefail
shopt -s globstar

source scripts/helpers.sh

## Overrides: Should be commented out in main
# RUNDECK_BUILD_NUMBER="4093"
# RUNDECK_TAG="v3.0.0-alpha4"

export ECR_REPO=481311893001.dkr.ecr.us-west-2.amazonaws.com/rundeck/rundeck
export ECR_REGISTRY=481311893001.dkr.ecr.us-west-2.amazonaws.com
export ECR_IMAGE_PREFIX=${ECR_IMAGE_PREFIX:-"circle"}

export RUNDECK_BUILD_NUMBER="${RUNDECK_BUILD_NUMBER:-$CI_BUILD_NUMBER}"
export RUNDECK_COMMIT="${RUNDECK_COMMIT:-$CI_COMMIT}"
export RUNDECK_BRANCH="${RUNDECK_BRANCH:-$CI_BRANCH}"
export RUNDECK_TAG="${RUNDECK_TAG:-$CI_TAG}"

export RUNDECK_PACKAGING_BRANCH="${RUNDECK_PACKAGING_BRANCH:-"main"}"

if [[ "${RUNDECK_BRANCH}" = "main" ]]; then
    export RUNDECK_MAIN_BUILD=true
else
    export RUNDECK_MAIN_BUILD=false
fi

# Location of CI resources such as private keys
S3_CI_RESOURCES="s3://rundeck-ci-resources/shared/resources"

# Locations we could push build artifacts to depending on release type (snapshot, alpha, ga, etc).
# The directory layout is designed to make browsing via the AWS console, and fetching from other projects easier.
S3_ARTIFACT_BASE="${S3_ARTIFACT_BASE:-"s3://rundeck-ci-artifacts/oss/rundeck"}"

S3_BUILD_ARTIFACT_PATH="${S3_ARTIFACT_BASE}/branch/${RUNDECK_BRANCH}/build/${RUNDECK_BUILD_NUMBER}/artifacts"
S3_BUILD_ARTIFACT_SEAL="${S3_ARTIFACT_BASE}/branch/${RUNDECK_BRANCH}/build-seal/${RUNDECK_BUILD_NUMBER}"
S3_COMMIT_ARTIFACT_PATH="${S3_ARTIFACT_BASE}/branch/${RUNDECK_BRANCH}/commit/${RUNDECK_COMMIT}/artifacts"
S3_TAG_ARTIFACT_PATH="${S3_ARTIFACT_BASE}/tag/${RUNDECK_TAG}/artifacts"
S3_LATEST_ARTIFACT_PATH="s3://rundeck-ci-artifacts/oss/circle/latest/artifacts"

# Store artifacts in a predictable location in the case of version tags.
# This will allow us to locate them across repos without passing information explicitly.
S3_ARTIFACT_PATH="${S3_BUILD_ARTIFACT_PATH}"
if [[ "${RUNDECK_TAG}" =~ ^v ]] ; then
    S3_ARTIFACT_PATH="${S3_TAG_ARTIFACT_PATH}"
fi

mkdir -p artifacts/packaging

# Exports tag info for current CI_TAG and previous tag
export_tag_info() {
    local TAG_PARTS
    if [[ ${TAG_PARTS=$(tag_parts "${CI_TAG}")} && $? != 0 ]] ; then
        echo "Invalid tag [${CI_TAG}]"
    else
        TAG_PARTS=( $TAG_PARTS )
    fi

    local PREV_TAG_PARTS=( $(tag_parts `git describe --abbrev=0 --tags $(git describe --abbrev=0)^ `) )

    export RUNDECK_RELEASE_VERSION="${TAG_PARTS[0]}"
    export RUNDECK_RELEASE_TAG="${TAG_PARTS[1]:-SNAPSHOT}"

    export RUNDECK_PREV_RELEASE_VERSION="${PREV_TAG_PARTS[0]}"
    export RUNDECK_PREV_RELEASE_TAG="${PREV_TAG_PARTS[1]:-SNAPSHOT}"
}


sync_from_s3() {
    aws s3 sync --delete "${S3_ARTIFACT_PATH}" artifacts
}

sync_to_s3() {
    aws s3 sync --delete ./artifacts "${S3_ARTIFACT_PATH}"
}

sync_to_latest_s3() {
    if [[ "${RUNDECK_BRANCH}" = "main" ]]; then
        aws s3 rm "${S3_LATEST_ARTIFACT_PATH}" --recursive --include "latest/*"
        aws s3 sync --delete ./artifacts "${S3_LATEST_ARTIFACT_PATH}"
    else
        echo "not main branch, not posting to latest"
    fi
}

sync_commit_from_s3() {
    aws s3 sync --delete "${S3_COMMIT_ARTIFACT_PATH}" artifacts
}

sync_commit_to_s3() {
    aws s3 sync --delete ./artifacts "${S3_COMMIT_ARTIFACT_PATH}"
}

copy_artifacts() {
    if [[ ! -d artifacts ]]; then mkdir artifacts; fi
    (
        # find ./packaging -regex '.*\.\(deb\|rpm\)' -exec cp --parents {} artifacts \;
        cp -r --parents core/build/libs artifacts/
        cp -r --parents core/build/publications/rundeck-core artifacts/
        cp -r --parents rundeckapp/**/build/libs artifacts/
        cp -r --parents rundeckapp/**/build/publications/rundeck artifacts/
        cp -r --parents rundeck-storage/**/build/libs artifacts/
        cp -r --parents rundeck-storage/**/build/publications/rundeck-storage-api artifacts/
        cp -r --parents rundeck-storage/**/build/publications/rundeck-storage-data artifacts/
        cp -r --parents rundeck-storage/**/build/publications/rundeck-storage-filesys artifacts/
        cp -r --parents rundeck-storage/**/build/publications/rundeck-storage-conf artifacts/
        
        cp -r --parents rundeck-authz/**/build/libs artifacts/
        cp -r --parents rundeck-authz/**/build/publications/rundeck-authz-api artifacts/
        cp -r --parents rundeck-authz/**/build/publications/rundeck-authz-core artifacts/
        cp -r --parents rundeck-authz/**/build/publications/rundeck-authz-yaml artifacts/
        tar -czf artifacts/m2.tgz -C ~/.m2/repository/ org/rundeck
    )
}

extract_artifacts() {
    # Drop to subshell and change dir; courtesy roundup
    (
        cd artifacts
        cp -r --parents * ../
    )
}

# Add marker to indicate artifacts are whole
seal_artifacts() {
    echo -n | aws s3 cp - "${S3_BUILD_ARTIFACT_SEAL}"
}

# Helper function that syncs artifacts from s3
# and copies the most common into place.
fetch_common_artifacts() {
    sync_from_s3
    extract_artifacts
}

fetch_common_resources() {
    aws s3 sync --delete "${S3_CI_RESOURCES}" ci-resources
}

fetch_commit_common_artifacts() {
    sync_commit_from_s3
    extract_artifacts
}

docker_login() {
      docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD
      aws ecr get-login-password --region us-west-2 | docker login --username AWS --password-stdin 481311893001.dkr.ecr.us-west-2.amazonaws.com

}

build_rdtest() {
    local buildJar=( $PWD/rundeckapp/build/libs/*.war )
    cp ${buildJar[0]} test/docker/rundeck-launcher.war

    (
        set -e
        cd test/docker/
        source common.sh
        build_rdtest_docker
    )

    # Tag and push to be used as cache source in later test builds
    docker_login

    # Pusheen
    if [[ -v ${CI_PULL_REQUEST} && "${CI_BRANCH}" == 'main' ]]; then
        docker tag rdtest:latest $ECR_REGISTRY/rundeck/rdtest:latest
        docker push $ECR_REGISTRY/rundeck/rdtest:latest
    fi

    local RDTEST_BUILD_TAG=$ECR_REGISTRY/rundeck/rdtest:${ECR_IMAGE_PREFIX}-build-${RUNDECK_BUILD_NUMBER}

    docker tag rdtest:latest $RDTEST_BUILD_TAG
    # docker tag rdtest:latest rundeckapp/testdeck:rdtest-${RUNDECK_BRANCH}
    docker push $RDTEST_BUILD_TAG
    # docker push rundeckapp/testdeck:rdtest-${RUNDECK_BRANCH}
}

pull_rdtest() {
    docker_login

    local RDTEST_BUILD_TAG=$ECR_REGISTRY/rundeck/rdtest:${ECR_IMAGE_PREFIX}-build-${RUNDECK_BUILD_NUMBER}

    docker pull $RDTEST_BUILD_TAG
    docker tag $RDTEST_BUILD_TAG rdtest:latest
}

pull_rundeck() {
    docker_login

    local ECR_BUILD_TAG=${ECR_REPO}:${ECR_IMAGE_PREFIX}-build-${RUNDECK_BUILD_NUMBER}

    docker pull $ECR_BUILD_TAG
    docker tag $ECR_BUILD_TAG rundeck/rundeck
}

twistlock_scan() {
    echo "==> Git Branch: $CIRCLE_BRANCH"
    echo "==> Git Tag: $CIRCLE_TAG"

    if [[ ! -z "${CIRCLE_TAG:-}" ]] ; then
        #If the build is triggered by a git Tag
        export RUNDECK_IMAGE_TAG="rundeck/rundeck:$CIRCLE_TAG"
    elif [[ "${CIRCLE_BRANCH}" = "main" ]] ; then
        export RUNDECK_IMAGE_TAG="rundeck/rundeck:SNAPSHOT"
    else
        export RUNDECK_IMAGE_TAG="rundeck/ci:$CIRCLE_BRANCH"
    fi

    echo "==> Scan Image: $RUNDECK_IMAGE_TAG"

    docker pull $RUNDECK_IMAGE_TAG

    ./twistcli images scan --details -address ${TL_CONSOLE_URL} -u ${TL_USER} -p ${TL_PASS} --output-file twistlock_scan_result.json $RUNDECK_IMAGE_TAG

    local severity=("low" "medium" "high" "critical")
    #report severity filter to extract incidents count, default: high and critical
    local reportSeverityFilter='.results[0].vulnerabilityDistribution.high + .results[0].vulnerabilityDistribution.critical'

    if [[ ! -z "${TWISTLOCK_SEVERITY_THRESHOLD:-}" && $TWISTLOCK_SEVERITY_THRESHOLD -ge 0 && $TWISTLOCK_SEVERITY_THRESHOLD -lt 4 ]] ; then
      reportSeverityFilter=""
      for sev in ${severity[@]:$TWISTLOCK_SEVERITY_THRESHOLD} ; do
          if [[ ! -z "$reportSeverityFilter" ]]; then
              reportSeverityFilter="$reportSeverityFilter + .results[0].vulnerabilityDistribution.$sev"
          else
              reportSeverityFilter=".results[0].vulnerabilityDistribution.$sev"
          fi
      done
    fi


    local incidents=$(cat twistlock_scan_result.json | jq "$reportSeverityFilter")

    if [[ $incidents > 0 ]] ; then
      echo "==> Security Alert: found vulnerabilities, $incidents of them must be mitigated before release. Please refer to the above report for detail."
      exit $incidents
    fi
}

export_tag_info

export -f sync_to_s3
export -f sync_commit_to_s3
export -f sync_from_s3
export -f sync_commit_from_s3
export -f fetch_common_artifacts
export -f docker_login
export -f build_rdtest
export -f pull_rdtest
export -f pull_rundeck
export -f twistlock_scan
