#!/bin/bash

# common header for test scripts

SRC_DIR=$(cd `dirname $0` && pwd)
DIR=${TMP_DIR:-$SRC_DIR}

errorMsg() {
   echo "$*" 1>&2
}
fail(){
    errorMsg "FAIL: $@"
    exit 2
}
assert(){
    # assert expected, actual
    if [ "$1" != "$2" ] ; then
        fail "Expected value \"$1\" but saw: \"$2\" ${3}"
    fi
}

# modify to point to xmlstarlet
XMLSTARLET=${XMLSTARLET:-xmlstarlet}

RDECK_PROJECTS=${RDECK_PROJECTS:-$RDECK_BASE/projects}
RDECK_ETC=${RDECK_ETC:-$RDECK_BASE/etc}
RDECK_URL=$(grep framework.server.url $RDECK_ETC/framework.properties  | cut -d' ' -f3)

# xmlstarlet select xpath
# usage: xmlsel XPATH file
xmlsel(){
    xpath=$1
    shift
    $XMLSTARLET sel -T -t -v "$xpath" $*
}

API_CURRENT_VERSION=9

API_VERSION=${API_VERSION:-$API_CURRENT_VERSION}


# curl opts to use a cookie jar, and follow redirects, showing only errors
if [ -n "$RDAUTH" ] ; then 
    AUTHHEADER="X-RunDeck-Auth-Token: $RDAUTH"
    CURLOPTS="-s -S -L"
else
    CURLOPTS="-s -S -L -c $DIR/cookies -b $DIR/cookies"
fi
CURL="curl $CURLOPTS"
docurl(){
    if [ -n "$RDAUTH" ] ; then
        if [ "true" == "$RDDEBUG" ] ; then
            echo $CURL -H "$AUTHHEADER" $* 1>&2
        fi
        $CURL -H "$AUTHHEADER" $*
    else    
        if [ "true" == "$RDDEBUG" ] ; then
            echo $CURL $* 1>&2
        fi
        $CURL $*
    fi
}

# accept url argument on commandline, if '-' use default
RDURL="$1"
if [ "-" == "$1" ] ; then
    RDURL=${RDECK_URL:-http://localhost:4440}
fi
shift

APIURL="${RDURL}/api/${API_VERSION}"
