#!/usr/bin/env bash
# Exports environment variables and functions for use in CI environments
#
# Exported variables
# ==================
# RUNDECK_BUILD_NUMBER = build number from TRAVIS_BUILD_NUMBER
# RUNDECK_TAG = Git tag used for this build and extracted from TRAVIS_TAG
# RUNDECK_RELEASE_TAG = (SNAPSHOT|GA|other) extracted from TRAVIS_TAG
# RUNDECK_RELEASE_VERSION = version number extracted from TRAVIS_TAG
# RUNDECK_PREV_RELEASE_VERSION = same as above extracted from second oldest tag in git history
# RUNDECK_PREV_RELEASE_VERSION = same as above extracted from second oldest tag in git history
# RUNDECK_MASTER_BUILD = (true|false) resolves Travis funkyness to determine if this is a true master build

# BINTRAY_DEB_REPO = selected deb bintray repo based on release tag
# BINTRAY_RPM_REPO = selected rpm bintray repo based on release tag
# BINTRAY_MAVEN_REPO = selected maven(jar,war) bintray repo based on release tag

set -eo pipefail
shopt -s globstar

source scripts/helpers.sh

## Overrides: Should be commented out in master
# RUNDECK_BUILD_NUMBER="4093"
# RUNDECK_TAG="v3.0.0-alpha4"

export ECR_REPO=481311893001.dkr.ecr.us-west-2.amazonaws.com/rundeck/rundeck
export ECR_REGISTRY=481311893001.dkr.ecr.us-west-2.amazonaws.com


export RUNDECK_BUILD_NUMBER="${RUNDECK_BUILD_NUMBER:-$TRAVIS_BUILD_NUMBER}"
export RUNDECK_COMMIT="${RUNDECK_COMMIT:-$TRAVIS_COMMIT}"
export RUNDECK_BRANCH="${RUNDECK_BRANCH:-$TRAVIS_BRANCH}"
export RUNDECK_TAG="${RUNDECK_TAG:-$TRAVIS_TAG}"

if [[ "${TRAVIS_EVENT_TYPE:-}" = "push" && "${RUNDECK_BRANCH}" = "master" ]]; then
    export RUNDECK_MASTER_BUILD=true
else
    export RUNDECK_MASTER_BUILD=false
fi

# Location of CI resources such as private keys
S3_CI_RESOURCES="s3://rundeck-ci-resources/shared/resources"

# Locations we could push build artifacts to depending on release type (snapshot, alpha, ga, etc).
# The directory layout is designed to make browsing via the AWS console, and fetching from other projects easier.
S3_ARTIFACT_BASE="s3://rundeck-ci-artifacts/oss/rundeck"

S3_BUILD_ARTIFACT_PATH="${S3_ARTIFACT_BASE}/branch/${RUNDECK_BRANCH}/build/${RUNDECK_BUILD_NUMBER}/artifacts"
S3_BUILD_ARTIFACT_SEAL="${S3_ARTIFACT_BASE}/branch/${RUNDECK_BRANCH}/build-seal/${RUNDECK_BUILD_NUMBER}"
S3_COMMIT_ARTIFACT_PATH="${S3_ARTIFACT_BASE}/branch/${RUNDECK_BRANCH}/commit/${RUNDECK_COMMIT}/artifacts"
S3_TAG_ARTIFACT_PATH="${S3_ARTIFACT_BASE}/tag/${RUNDECK_TAG}/artifacts"

# Store artifacts in a predictable location in the case of version tags.
# This will allow us to locate them across repos without passing information explicitly.
S3_ARTIFACT_PATH="${S3_BUILD_ARTIFACT_PATH}"
if [[ "${RUNDECK_TAG}" =~ ^v ]] ; then
    S3_ARTIFACT_PATH="${S3_TAG_ARTIFACT_PATH}"
fi

mkdir -p artifacts/packaging

# Exports tag info for current TRAVIS_TAG and previous tag
export_tag_info() {
    local TAG_PARTS
    if [[ ${TAG_PARTS=$(tag_parts "${TRAVIS_TAG}")} && $? != 0 ]] ; then
        echo "Invalid tag [${TRAVIS_TAG}]"
    else
        TAG_PARTS=( $TAG_PARTS )
    fi

    local PREV_TAG_PARTS=( $(tag_parts `git describe --abbrev=0 --tags $(git describe --abbrev=0)^ `) )

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
    local NAME="${1}"; shift;
    local COMMAND="${@}"

    # Return from 
    local ret

    travis_fold start "${NAME}"
        travis_time_start
            eval "${COMMAND}"
            ret=$?
            echo "Command [${COMMAND}] returned ${ret}"
        travis_time_finish
    travis_fold end "${NAME}"
    return $ret
}

sync_from_s3() {
    aws s3 sync --delete "${S3_ARTIFACT_PATH}" artifacts
}

sync_to_s3() {
    aws s3 sync --delete ./artifacts "${S3_ARTIFACT_PATH}"
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
        find ./packaging -regex '.*\.\(deb\|rpm\)' -exec cp --parents {} artifacts \;
        cp -r --parents core/build/libs artifacts/
        cp -r --parents core/build/poms artifacts/
        cp -r --parents rundeckapp/**/build/libs artifacts/
        cp -r --parents rundeckapp/**/build/poms artifacts/
        cp -r --parents rundeck-storage/**/build/libs artifacts/
        cp -r --parents rundeck-storage/**/build/poms artifacts/
        tar -czf artifacts/m2.tgz -C ~/.m2/repository/ org/rundeck metricsweb
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

trigger_travis_build() {
    local token="${1:?Must supply token}"
    local travis_flav="${2:?Must supply org or com}"
    local owner="${3:?Must supply owner}"
    local repo="${4:?Must supply repo}"
    local branch="${5:?Must spupply branch}"

    local body=$(cat <<EOF
    {
        "request": {
            "branch": "${branch}",
            "message": "Rundeck OSS triggered build.",
            "config": {
                "merge_mode": "deep_merge",
                "env": {
                    "UPSTREAM_PROJECT": "rundeck",
                    "UPSTREAM_BUILD_NUMBER": "${RUNDECK_BUILD_NUMBER}",
                    "UPSTREAM_BRANCH": "${RUNDECK_BRANCH}",
                    "UPSTREAM_TAG": "${RUNDECK_TAG}"
                }
            }
        }
    }
EOF
)

    curl -s -X POST \
        -H "Content-Type: application/json" \
        -H "Accept: application/json" \
        -H "Travis-API-Version: 3" \
        -H "Authorization: token ${token}" \
        -d "$body" \
        https://api.travis-ci.${travis_flav}/repo/${owner}%2F${repo}/requests
}

docker_login() {
    docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD
    $(aws ecr get-login --no-include-email --region us-west-2)
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
    if [[ "${TRAVIS_PULL_REQUEST}" != 'false' && "${TRAVIS_BRANCH}" == 'master' ]]; then
        docker tag rdtest:latest $ECR_REGISTRY/rundeck/rdtest:latest
        docker push $ECR_REGISTRY/rundeck/rdtest:latest
    fi

    local RDTEST_BUILD_TAG=$ECR_REGISTRY/rundeck/rdtest:build-${RUNDECK_BUILD_NUMBER}

    docker tag rdtest:latest $RDTEST_BUILD_TAG
    # docker tag rdtest:latest rundeckapp/testdeck:rdtest-${RUNDECK_BRANCH}
    docker push $RDTEST_BUILD_TAG
    # docker push rundeckapp/testdeck:rdtest-${RUNDECK_BRANCH}
}

pull_rdtest() {
    docker_login

    local RDTEST_BUILD_TAG=$ECR_REGISTRY/rundeck/rdtest:build-${RUNDECK_BUILD_NUMBER}

    docker pull $RDTEST_BUILD_TAG
    docker tag $RDTEST_BUILD_TAG rdtest:latest
}

pull_rundeck() {
    docker_login

    local ECR_BUILD_TAG=${ECR_REPO}:build-${RUNDECK_BUILD_NUMBER}

    docker pull $ECR_BUILD_TAG
    docker tag $ECR_BUILD_TAG rundeck/rundeck
}

# If this is a snapshot build we will trigger pro
trigger_downstream_snapshots() {
    if [[ -z "${RUNDECK_TAG}" && "${RUNDECK_BRANCH}" == "master" && "${TRAVIS_EVENT_TYPE}" == "push" ]] ; then
        echo "Triggering downstream snapshot build..."
        seal_artifacts
        trigger_travis_build "${TRAVIS_RDPRO_TOKEN}" com rundeckpro rundeckpro master
        trigger_travis_build "${TRAVIS_OSS_TOKEN}" org rundeck packaging-core master
    else
        echo "Skippping downstream snapshot build for non-master/snapshot build..."
    fi
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
export -f docker_login
export -f build_rdtest
export -f pull_rdtest
export -f pull_rundeck
export -f trigger_downstream_snapshots