#!/bin/bash

CUR_VERSION=$(grep version.number= `pwd`/version.properties | cut -d= -f 2)
CUR_RELEASE=$(grep version.release.number= `pwd`/version.properties | cut -d= -f 2)

echo "current NUMBER: $CUR_VERSION"
echo "current RELEASE: $CUR_RELEASE"

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
if [ -n "$1" ]; then
    TAG=
else
    TAG="-SNAPSHOT"
fi

VNAME="${VERSION}"

echo "new NUMBER: $VERSION${TAG}"
echo "new RELEASE: $RELEASE"
echo "new VERSION: ${VNAME}"

#alter version.properties
perl  -i'.orig' -p -e "s#^version\.number\s*=.*\$#version.number=$VERSION#" `pwd`/version.properties
perl  -i'.orig' -p -e "s#^version\.release\.number\s*=.*\$#version.release.number=$RELEASE#" `pwd`/version.properties

perl  -i'.orig' -p -e "s#^currentVersion\s*=.*\$#currentVersion = $VERSION#" `pwd`/gradle.properties

echo MODIFIED: `pwd`/version.properties

perl  -i'.orig' -p -e "s#^app.version\s*=.*\$#app.version = $VERSION${TAG}#" `pwd`/rundeckapp/application.properties
perl  -i'.orig' -p -e "s#^build.ident\s*=.*\$#build.ident = $VERSION-${RELEASE}${TAG}#" `pwd`/rundeckapp/application.properties

#alter pom.xml version

XML=$(which xmlstarlet)
if [ -z "$XML" ] ; then
    XML=$(which xml)
fi

$XML ed -P -S -N p=http://maven.apache.org/POM/4.0.0 -u "/p:project/p:version" -v "${VNAME}${TAG}" pom.xml > pom_new.xml
mv pom_new.xml pom.xml

$XML ed -P -S -N p=http://maven.apache.org/POM/4.0.0 -u "/p:project/p:version" -v "${VNAME}${TAG}" rundeckapp/pom.xml > rundeckapp/pom_new.xml
mv rundeckapp/pom_new.xml rundeckapp/pom.xml

echo MODIFIED: `pwd`/rundeckapp/pom.xml
if [ "$TAG" == "-SNAPSHOT" ]; then
    ./gradlew -PbuildNum=${RELEASE} createPom
else
    ./gradlew -PbuildNum=${RELEASE} -Penvironment=release createPom
fi
