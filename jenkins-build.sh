#!/bin/bash

set -e
set -x

# script for use by jenkins to build rundeck
# arguments: ['release' [tag]]
# if the first argument is 'release', a release build will be made, with an optional release tag

REL="${1:-no}"
RTAG="${2:-GA}"

export BUILD_ROOT=$WORKSPACE/build_root


echo "RTAG: ${RTAG}"

make clean

if [ "$REL" = "release" ] ; then
    ./gradlew -Penvironment=release -PreleaseTag=${RTAG} build
    groovy testbuild.groovy -gradle -Drelease -DreleaseTag=${RTAG}
else
    ./gradlew -Penvironment=build build
    groovy testbuild.groovy -gradle
fi

make TAG=${RTAG} rpm deb
