#!/bin/bash

CUR_VERSION=$(grep version.number= `pwd`/version.properties | cut -d= -f 2)
CUR_RELEASE=$(grep version.release.number= `pwd`/version.properties | cut -d= -f 2)
CUR_TAG=$(grep version.tag= `pwd`/version.properties | cut -d= -f 2)

echo "current NUMBER: $CUR_VERSION"
echo "current RELEASE: $CUR_RELEASE"
echo "current TAG: $CUR_TAG"

if [ -z "$1" ] ; then
echo "usage: setversion.sh <version> [release] [GA]"
exit 2
fi

VERSION=$1

shift
if [ -z "$1" ]; then
    RELEASE=$CUR_RELEASE
else
    RELEASE=$1
fi
shift
if [ "$1" = "GA" ]; then
    TAG=
    PTAG="GA"
elif [ -n "$1" ]; then
    TAG="-$1"
    PTAG="$1"
else
    TAG="-SNAPSHOT"
    PTAG="SNAPSHOT"
fi

VNAME="${VERSION}"

echo "new NUMBER: $VERSION${TAG}"
echo "new RELEASE: $RELEASE"
echo "new VERSION: ${VNAME}"

#alter version.properties
perl  -i'.orig' -p -e "s#^version\.number\s*=.*\$#version.number=$VERSION#" `pwd`/version.properties
perl  -i'.orig' -p -e "s#^version\.release\.number\s*=.*\$#version.release.number=$RELEASE#" `pwd`/version.properties
perl  -i'.orig' -p -e "s#^version\.tag\s*=.*\$#version.tag=$PTAG#" `pwd`/version.properties

perl  -i'.orig' -p -e "s#^currentVersion\s*=.*\$#currentVersion = $VERSION#" `pwd`/gradle.properties

echo MODIFIED: `pwd`/version.properties

perl  -i'.orig' -p -e "s#^app.version\s*=.*\$#app.version = $VERSION${TAG}#" `pwd`/rundeckapp/application.properties
perl  -i'.orig' -p -e "s#^build.ident\s*=.*\$#build.ident = $VERSION-${RELEASE}${TAG}#" `pwd`/rundeckapp/application.properties
