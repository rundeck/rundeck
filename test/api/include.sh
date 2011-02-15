#!/bin/bash

# common header for test scripts

DIR=$(cd `dirname $0` && pwd)

errorMsg() {
   echo "$*" 1>&2
}
assert(){
    # assert expected, actual
    if [ "$1" != "$2" ] ; then
        errorMsg "FAIL: Expected value \"$1\" but saw: \"$2\" ${3}"
        exit 2
    fi
}

# modify to point to xmlstarlet
XMLSTARLET=xml

# xmlstarlet select xpath
# usage: xmlsel XPATH file
xmlsel(){
    $XMLSTARLET sel -T -t -v "$1" $2
}

API_VERSION="1"

# curl opts to use a cookie jar, and follow redirects, showing only errors
CURLOPTS="-s -S -L -c $DIR/cookies -b $DIR/cookies"
CURL="curl $CURLOPTS"

# accept url argument on commandline, if '-' use default
RDURL="$1"
if [ "-" == "$1" ] ; then
    RDURL='http://localhost:4440'
fi
shift

APIURL="${RDURL}/api/${API_VERSION}"