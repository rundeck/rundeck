#!/usr/bin/env bash
# Exports environment variables and functions for use in CI environments
set -eo pipefail

export CI_BRANCH=${CIRCLE_BRANCH:-}
export CI_TAG=${CIRCLE_TAG:-}
export CI_BUILD_NUMBER=${CIRCLE_PIPELINE_NUM:-}
export CI_COMMIT=${CIRCLE_SHA1:-}
export CI_PULL_REQUEST=${CIRCLE_PULL_REQUEST:-}

S3_ARTIFACT_BASE="s3://rundeck-ci-artifacts/oss/circle/rundeck"
ECR_IMAGE_PREFIX="circle"

source scripts/circle-helpers.sh
