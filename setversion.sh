#!/bin/bash

CUR_VERSION=$(grep version.number= `pwd`/version.properties | cut -d= -f 2)
CUR_RELEASE=$(grep version.release.number= `pwd`/version.properties | cut -d= -f 2)
CUR_TAG=$(grep version.tag= `pwd`/version.properties | cut -d= -f 2)

echo "current VERSION: $CUR_VERSION"
echo "current RELEASE: $CUR_RELEASE"
echo "current TAG: $CUR_TAG"

if [ -z "$1" ] ; then
echo "usage: setversion.sh <version> [release] [tag]"
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
if [ -z "$1" ]; then
    TAG=$CUR_TAG
else
    TAG=$1
fi

IDENT_TAG="-$TAG"
if [ "$TAG" = "GA" ] ; then
    IDENT_TAG=
fi

echo "new VERSION: $VERSION"
echo "new RELEASE: $RELEASE"
echo "new TAG: $TAG"

#alter version.properties
perl  -i'.orig' -p -e "s#^version\.number\s*=.*\$#version.number=$VERSION#" `pwd`/version.properties
perl  -i'.orig' -p -e "s#^version\.release\.number\s*=.*\$#version.release.number=$RELEASE#" `pwd`/version.properties
perl  -i'.orig' -p -e "s#^version\.tag\s*=.*\$#version.tag=$TAG#" `pwd`/version.properties

echo MODIFIED: `pwd`/version.properties

#alter grails i18n messages main.app.version.num=1.0.0
perl  -i'.orig' -p -e "s#^app\.version\s*=.*\$#app.version=$VERSION#" `pwd`/rundeckapp/application.properties
perl  -i'.orig' -p -e "s#^build\.ident\s*=.*\$#build.ident=$VERSION-$RELEASE$IDENT_TAG#" `pwd`/rundeckapp/application.properties

echo MODIFIED: `pwd`/rundeckapp/application.properties

#modify core/build.gradle
perl  -i'.orig' -p -e "s#^version\s*=.*\$#version = '$VERSION'#" `pwd`/core/build.gradle

echo MODIFIED: `pwd`/core/build.gradle

#modify plugins/build.gradle
perl  -i'.orig' -p -e "s#^version\s*=.*\$#version = '$VERSION'#" `pwd`/plugins/build.gradle

echo MODIFIED: `pwd`/plugins/build.gradle
