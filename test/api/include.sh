#!/bin/bash

# common header for test scripts

DIR=$(cd `dirname $0` && pwd)

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
XMLSTARLET=xml

# xmlstarlet select xpath
# usage: xmlsel XPATH file
xmlsel(){
    xpath=$1
    shift
    $XMLSTARLET sel -T -t -v "$xpath" $*
}

API_CURRENT_VERSION=6

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
    RDURL='http://localhost:4440'
fi
shift

APIURL="${RDURL}/api/${API_VERSION}"
