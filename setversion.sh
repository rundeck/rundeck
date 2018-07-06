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

VNUM=$1

shift
if [ -z "$1" ]; then
    RELEASE=$CUR_RELEASE
else
    RELEASE=$1
fi
shift
VTAG="${1}"


VDATE=`date +%Y%m%d`
VNAME=${VNUM}-${VTAG}-${VDATE}

echo "new NUMBER: ${VNUM}"
echo "new RELEASE: ${RELEASE}"
echo "new DATE: ${VDATE}"
echo "new VERSION: ${VNAME}"

#alter version.properties
perl  -i'.orig' -p -e "s#^version\.number\s*=.*\$#version.number=$VNUM#" `pwd`/version.properties
perl  -i'.orig' -p -e "s#^version\.release\.number\s*=.*\$#version.release.number=$RELEASE#" `pwd`/version.properties
perl  -i'.orig' -p -e "s#^version\.tag\s*=.*\$#version.tag=$VTAG#" `pwd`/version.properties
perl  -i'.orig' -p -e "s#^version\.date\s*=.*\$#version.date=$VDATE#" `pwd`/version.properties
perl  -i'.orig' -p -e "s#^version\.version\s*=.*\$#version.version=$VNAME#" `pwd`/version.properties

perl  -i'.orig' -p -e "s#^currentVersion\s*=.*\$#currentVersion = $VNUM#" `pwd`/gradle.properties

echo MODIFIED: `pwd`/version.properties

