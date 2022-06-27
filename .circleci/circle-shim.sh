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

## Exports env variables to packaging pro
#export UPSTREAM_BUILD_NUMBER=${CIRCLE_UPSTREAM_PIPELINE_NUMBER:-}
#export UPSTREAM_BRANCH=${CIRCLE_UPSTREAM_BRANCH:-}
#export UPSTREAM_TAG=${CIRCLE_UPSTREAM_TAG:-}
#export UPSTREAM_ARTIFACT_BASE=${CIRCLE_UPSTREAM_ARTIFACT_BASE:-s3://rundeck-ci-artifacts/pro/circle}
#export UPSTREAM_PROJECT=${CIRCLE_UPSTREAM_PROJECT:-rundeckpro}
#
#export ECR_REPO=481311893001.dkr.ecr.us-west-2.amazonaws.com/rundeckpro/enterprise
#
## Exports env variables to packaging core
#export SIGNING_PASSWORD=${RUNDECK_SIGNING_PASSWORD}
#export SIGNING_KEYID=${RUNDECK_SIGNING_KEYID}