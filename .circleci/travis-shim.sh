#!/usr/bin/env bash
# Exports environment variables and functions for use in CI environments
set -eo pipefail

export TRAVIS_BRANCH=${CIRCLE_BRANCH:-}
export TRAVIS_TAG=${CIRCLE_TAG:-}
export TRAVIS_BUILD_NUMBER=${CIRCLE_PIPELINE_NUM:-}
export TRAVIS_COMMIT=${CIRCLE_SHA1:-}

S3_ARTIFACT_BASE="s3://rundeck-ci-artifacts/oss/circle/rundeck"
ECR_IMAGE_PREFIX="circle"

source scripts/travis-helpers.sh