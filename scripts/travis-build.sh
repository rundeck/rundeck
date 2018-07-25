#!/bin/bash

set -euo pipefail

ENV=development

if [[ ! -z "${RUNDECK_TAG:-}" ]] ; then
    ENV=release
fi

make ENV="${ENV}" rpm deb

groovy testbuild.groovy --buildType="${ENV}"