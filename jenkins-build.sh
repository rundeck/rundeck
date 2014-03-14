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

    # build with release tag
    ./gradlew -Penvironment=release -PreleaseTag=${RTAG} build

    # test artifact contents
    groovy testbuild.groovy -gradle -Drelease -DreleaseTag=${RTAG}

elif [ "$REL" = "upload" ] ; then

    # upload archives to sonatype nexus
    # need more memory otherwise OOM error uploading war file
    GRADLE_OPTS="-Xmx1024m -Xms256m" ./gradlew ${PROXY_DEFS} --no-daemon -Penvironment=release -PreleaseTag=${RTAG} \
      ${RELEASE_OPTS} uploadArchives

    # close nexus staging repos
    ./gradlew ${PROXY_DEFS} --no-daemon ${RELEASE_OPTS} nexusStagingRelease
    exit $?
else
    ./gradlew -Penvironment=build build
    groovy testbuild.groovy -gradle
fi

make TAG=${RTAG} rpm deb
