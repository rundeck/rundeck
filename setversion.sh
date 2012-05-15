#!/bin/bash

CUR_VERSION=$(grep version.number= `pwd`/version.properties | cut -d= -f 2)
CUR_RELEASE=$(grep version.release.number= `pwd`/version.properties | cut -d= -f 2)
CUR_TAG=$(grep version.tag= `pwd`/version.properties | cut -d= -f 2)

echo "current NUMBER: $CUR_VERSION"
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

VNAME="${VERSION}${IDENT_TAG}"

echo "new NUMBER: $VERSION"
echo "new RELEASE: $RELEASE"
echo "new TAG: $TAG"
echo "new VERSION: ${VNAME}"

#alter version.properties
perl  -i'.orig' -p -e "s#^version\.number\s*=.*\$#version.number=$VERSION#" `pwd`/version.properties
perl  -i'.orig' -p -e "s#^version\.release\.number\s*=.*\$#version.release.number=$RELEASE#" `pwd`/version.properties
perl  -i'.orig' -p -e "s#^version\.tag\s*=.*\$#version.tag=$TAG#" `pwd`/version.properties

echo MODIFIED: `pwd`/version.properties

#alter pom.xml version

XML=$(which xmlstarlet)
if [ -z "$XML" ] ; then
    XML=$(which xml)
fi
$XML ed -P -S -N p=http://maven.apache.org/POM/4.0.0 -u "/p:project/p:version" -v "${VNAME}" pom.xml > pom_new.xml
mv pom_new.xml pom.xml

echo MODIFIED: `pwd`/pom.xml

#alter grails i18n messages main.app.version.num=1.0.0
perl  -i'.orig' -p -e "s#^app\.version\s*=.*\$#app.version=${VNAME}#" `pwd`/rundeckapp/application.properties
perl  -i'.orig' -p -e "s#^build\.ident\s*=.*\$#build.ident=$VERSION-$RELEASE$IDENT_TAG#" `pwd`/rundeckapp/application.properties

echo MODIFIED: `pwd`/rundeckapp/application.properties

$XML ed -P -S -N p=http://maven.apache.org/POM/4.0.0 -u "/p:project/p:version" -v "${VNAME}" rundeckapp/pom.xml > rundeckapp/pom_new.xml
mv rundeckapp/pom_new.xml rundeckapp/pom.xml

echo MODIFIED: `pwd`/rundeckapp/pom.xml

#modify core/build.gradle
perl  -i'.orig' -p -e "s#^version\s*=.*\$#version = '$VNAME'#" `pwd`/core/build.gradle
cd core/ && ./gradlew -PbuildNum=${RELEASE} createPom && cd ..

echo MODIFIED: `pwd`/core/build.gradle

#modify plugins/build.gradle
perl  -i'.orig' -p -e "s#^(\s*)version\s*=.*\$#\1version = '$VNAME'#" `pwd`/plugins/build.gradle
cd plugins/ && ./gradlew createPom && cd ..

echo MODIFIED: `pwd`/plugins/build.gradle

#modify rundeck-launcher/build.gradle
perl  -i'.orig' -p -e "s#^(\s*)version\s*=.*\$#\1version = '$VNAME'#" `pwd`/rundeck-launcher/build.gradle
cd rundeck-launcher/ && ./gradlew createPom && cd ..

echo MODIFIED: `pwd`/rundeck-launcher/build.gradle
