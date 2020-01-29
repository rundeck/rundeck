#!/bin/bash

set -eo pipefail

source .circleci/travis-shim.sh
source scripts/travis-helpers.sh

echo "${@}"

eval "${@}"