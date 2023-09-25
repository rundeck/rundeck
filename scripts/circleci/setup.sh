#!/bin/bash
# Setup environment for circleci builds.
set -ueo pipefail

export CURRENT_USER="$(whoami)"
export WORKDIR=${WORKDIR:-${CIRCLE_WORKING_DIRECTORY:-"${HOME}/workspace"}}
export RUNDECK_CORE_DIR=${RUNDECK_CORE_DIR:-${WORKDIR}}

# Map circle env
export CI_BRANCH=${CI_BRANCH:-${CIRCLE_BRANCH:-}}
export CI_TAG=${CI_TAG:-${CIRCLE_TAG:-}}
export CI_BUILD_NUMBER=${CI_BUILD_NUMBER:-${CIRCLE_PIPELINE_NUM:-}}
export CI_COMMIT=${CI_COMMIT:-${CIRCLE_SHA1:-}}
export CI_PULL_REQUEST=${CI_PULL_REQUEST:-${CIRCLE_PULL_REQUEST:-}}

# Setup Rundeck git info
export RUNDECK_BUILD_NUMBER="${RUNDECK_BUILD_NUMBER:-$CI_BUILD_NUMBER}"
export RUNDECK_COMMIT="${RUNDECK_COMMIT:-$CI_COMMIT}"
export RUNDECK_BRANCH="${RUNDECK_BRANCH:-$CI_BRANCH}"
export RUNDECK_TAG="${RUNDECK_TAG:-$CI_TAG}"

export RUNDECK_BRANCH_CLEAN=$(echo "${RUNDECK_BRANCH}" | tr '/' '-')
export RUNDECK_TAG_CLEAN=$(echo "${RUNDECK_TAG}" | tr '/' '-')
export RUNDECK_PACKAGING_BRANCH="${RUNDECK_PACKAGING_BRANCH:-"main"}"
export RUNDECK_MAIN_BUILD=$([[ "${RUNDECK_BRANCH}" = "main" ]] && echo "true" || echo "false")

# Configurations
export ENV=$([[ -n "${RUNDECK_TAG}" ]] && echo "release" || echo "development")
export GRADLE_BASE_OPTS="--no-daemon --build-cache --stacktrace"
export GRADLE_BUILD_OPTS="${GRADLE_BASE_OPTS} --max-workers 4 --parallel"
export PACKAGING_DIR="${WORKDIR}/packaging"
export RUNDECK_WAR_DIR="${RUNDECK_CORE_DIR}/rundeckapp/build/libs"

# Docker Env set in CIRCLE config
export DOCKER_USERNAME=${DOCKER_USERNAME:-}
export DOCKER_PASSWORD=${DOCKER_PASSWORD:-}

# DockerHub
export DOCKER_REPO=${DOCKER_REPO:-"rundeck/rundeck"}
export DOCKER_CI_REPO=${DOCKER_CI_REPO:-"rundeck/ci"}

# Set Twistlock env and map with Circle ENV.
export TWISTLOCK_USER=${TWISTLOCK_USER:-${TL_USER:-}}
export TWISTLOCK_PASSWORD=${TWISTLOCK_PASSWORD:-${TL_PASS:-}}
export TWISTLOCK_CONSOLE_URL=${TWISTLOCK_CONSOLE_URL:-${TL_CONSOLE_URL:-}}

# AWS Config
export ECR_REGISTRY=481311893001.dkr.ecr.us-west-2.amazonaws.com
export ECR_REPO=${ECR_REGISTRY}/rundeck/rundeck
export ECR_TEST_REPO=${ECR_REGISTRY}/rundeck/rdtest
export ECR_IMAGE_PREFIX=${ECR_IMAGE_PREFIX:-"circle"}
export ECR_IMAGE_TAG=${ECR_IMAGE_PREFIX}-build-${RUNDECK_BUILD_NUMBER}
export S3_CI_SHARED_RESOURCES="s3://rundeck-ci-resources/shared/resources"

# Import functions
source "${RUNDECK_CORE_DIR}/scripts/circleci/helper-functions.sh"
source "${RUNDECK_CORE_DIR}/scripts/circleci/build-functions.sh"
source "${RUNDECK_CORE_DIR}/scripts/circleci/testdeck-functions.sh"
source "${RUNDECK_CORE_DIR}/scripts/circleci/packaging-functions.sh"
source "${RUNDECK_CORE_DIR}/scripts/circleci/dependencies-functions.sh"

# If local run, override functions that won't work locally.
if [[ $CIRCLE_SHELL_ENV == *"localbuild"* && $CIRCLE_LOCAL_BUILD == "true" ]]; then
    echo "Local Mode Detected!"
    source "${RUNDECK_CORE_DIR}/scripts/circleci/local-overrides.sh"
fi
