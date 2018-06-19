#!/usr/bin/env bash
# Exports environment variables and functions for use in CI environments
#
# Exported variables
# ==================
# RUNDECK_BUILD_NUMBER = build number from TRAVIS_BUILD_NUMBER
# RUNDECK_RELEASE_TAG = (SNAPSHOT|GA|other) extracted from TRAVIS_TAG
# RUNDECK_RELEASE_VERSION = version number extracted from TRAVIS_TAG
# RUNDECK_PREV_RELEASE_VERSION = same as above extracted from second oldest tag in git history
# RUNDECK_PREV_RELEASE_VERSION = same as above extracted from second oldest tag in git history
#
# BINTRAY_DEB_REPO = selected deb bintray repo based on release tag
# BINTRAY_RPM_REPO = selected rpm bintray repo based on release tag
# BINTRAY_MAVEN_REPO = selected maven(jar,war) bintray repo based on release tag

set -e

source scripts/helpers.sh

export RUNDECK_BUILD_NUMBER="${TRAVIS_BUILD_NUMBER}"
export RUNDECK_COMMIT="${TRAVIS_COMMIT}"

S3_BUILD_ARTIFACT_PATH="s3://rundeck-travis-artifacts/oss/${TRAVIS_BRANCH}/travis-builds/${RUNDECK_BUILD_NUMBER}/artifacts"
S3_COMMIT_ARTIFACT_PATH="s3://rundeck-travis-artifacts/oss/${TRAVIS_BRANCH}/commits/${RUNDECK_COMMIT}/artifacts"

S3_CI_RESOURCES="s3://rundeck-ci/shared/resources"

mkdir -p artifacts/packaging

# Exports tag info for current TRAVIS_TAG and previous tag
export_tag_info() {
    local TAG_PARTS
    if [[ ${TAG_PARTS=$(tag_parts "${TRAVIS_TAG}")} && $? != 0 ]] ; then
        echo "Invalid tag [${TRAVIS_TAG}]"
    else
        TAG_PARTS=( $TAG_PARTS )
    fi

    PREV_TAG_PARTS=( $(tag_parts `git describe --abbrev=0 --tags $(git describe --abbrev=0)^ `) )

    export RUNDECK_RELEASE_VERSION="${TAG_PARTS[0]}"
    export RUNDECK_RELEASE_TAG="${TAG_PARTS[1]:-SNAPSHOT}"

    export RUNDECK_PREV_RELEASE_VERSION="${PREV_TAG_PARTS[0]}"
    export RUNDECK_PREV_RELEASE_TAG="${PREV_TAG_PARTS[1]:-SNAPSHOT}"
}

export_repo_info() {
    local deb_repo=$(release_tag_repo deb ${RUNDECK_RELEASE_TAG})
    local rpm_repo=$(release_tag_repo rpm ${RUNDECK_RELEASE_TAG})
    local maven_repo=$(release_tag_repo maven ${RUNDECK_RELEASE_TAG})

    export BINTRAY_DEB_REPO=$deb_repo
    export BINTRAY_RPM_REPO=$rpm_repo
    export BINTRAY_MAVEN_REPO=$maven_repo
}

# Wraps bash command in code folding and timing "stamps"
script_block() {
    NAME="${1}"

    # Remove block name from arg array
    shift

    travis_fold start "${NAME}"
        travis_time_start
            eval "${@}"
            ret=$?
            echo "Command [${@}] returned ${ret}"
        travis_time_finish
    travis_fold end "${NAME}"
    return $RET
}

sync_from_s3() {
    aws s3 sync --delete "${S3_BUILD_ARTIFACT_PATH}" artifacts
}

sync_to_s3() {
    aws s3 sync --delete ./artifacts "$S3_BUILD_ARTIFACT_PATH"
}

sync_commit_from_s3() {
    aws s3 sync --delete "${S3_COMMIT_ARTIFACT_PATH}" artifacts
}

sync_commit_to_s3() {
    aws s3 sync --delete ./artifacts "$S3_COMMIT_ARTIFACT_PATH"
}

extract_artifacts() {
    # Drop to subshell and change dir; courtesy roundup
    (
        cd artifacts
        cp -r --parents * ../
    )
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

trigger_travis_build() {
    local token="${1:?Must supply token}"
    local owner="${2:?Must supply owner}"
    local repo="${3:?Must supply repo}"
    local branch="${4:?Must spupply branch}"

    local body="{
        \"request\": {
            \"branch\":\"${branch}\",
            \"message\": \"Rundeck OSS triggered build.\"
        }
    }"

    curl -s -X POST \
        -H "Content-Type: application/json" \
        -H "Accept: application/json" \
        -H "Travis-API-Version: 3" \
        -H "Authorization: token ${token}" \
        -d "$body" \
        https://api.travis-ci.com/repo/${owner}%2F${repo}/requests
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
    docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD
    docker tag rdtest:latest rundeckapp/testdeck:rdtest-latest
    docker push rundeckapp/testdeck:rdtest-latest
}

pull_rdtest() {
    docker pull rundeckapp/testdeck:rdtest-latest
}

export_tag_info
export_repo_info

export -f script_block
export -f sync_to_s3
export -f sync_commit_to_s3
export -f sync_from_s3
export -f sync_commit_from_s3
export -f fetch_common_artifacts
export -f trigger_travis_build
export -f build_rdtest
export -f pull_rdtest