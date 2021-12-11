#!/bin/bash

set -eo pipefail

source .circleci/circle-shim.sh
source scripts/circle-helpers.sh

echo "${@}"

eval "${@}"