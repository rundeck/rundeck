#!/bin/bash

CUR_VERSION="$(grep version.number= "$PWD/version.properties" | cut -d= -f 2)"
CUR_RELEASE="$(grep version.release.number= "$PWD/version.properties" | cut -d= -f 2)"
CUR_TAG="$(grep version.tag= "$PWD/version.properties" | cut -d= -f 2)"

echo "current NUMBER: $CUR_VERSION"
echo "current RELEASE: $CUR_RELEASE"
echo "current TAG: $CUR_TAG"

if [ -z "$1" ] ; then
echo "usage: setversion.sh <version> [release] [GA]"
exit 2
fi

VNUM="$1"

shift
if [ -z "$1" ]; then
    RELEASE="$CUR_RELEASE"
else
    RELEASE="$1"
fi
shift
VTAG="${1:-GA}"


VDATE="$(date +%Y%m%d)"

if [ "$VTAG" == "GA" ] ; then
	VNAME="$VNUM-$VDATE"
else
	VNAME="$VNUM-$VTAG-$VDATE"
fi

echo "new NUMBER: $VNUM"
echo "new RELEASE: $RELEASE"
echo "new DATE: $VDATE"
echo "new VERSION: $VNAME"

#alter version.properties
perl  -i'.orig' -p -e "s#^version\\.number\\s*=.*\$#version.number=$VNUM#" "$PWD/version.properties"
perl  -i'.orig' -p -e "s#^version\\.release\\.number\\s*=.*\$#version.release.number=$RELEASE#" "$PWD/version.properties"
perl  -i'.orig' -p -e "s#^version\\.tag\\s*=.*\$#version.tag=$VTAG#" "$PWD/version.properties"
perl  -i'.orig' -p -e "s#^version\\.date\\s*=.*\$#version.date=$VDATE#" "$PWD/version.properties"
perl  -i'.orig' -p -e "s#^version\\.version\\s*=.*\$#version.version=$VNAME#" "$PWD/version.properties"

perl  -i'.orig' -p -e "s#^currentVersion\\s*=.*\$#currentVersion = $VNUM#" "$PWD"/gradle.properties

echo MODIFIED: "$(pwd)"/version.properties

