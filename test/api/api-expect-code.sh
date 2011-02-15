#!/bin/bash

# usage:
#  api-expect-code.sh <code> <URL> <params> <message>
# curls the URL with the params, and expects certain HTTP response code

errorMsg() {
   echo "$*" 1>&2
}

DIR=$(cd `dirname $0` && pwd)

ecode="$1"
shift

requrl="$1"
shift

# accept url argument on commandline, if '-' use default
VERSHEADER="X-RUNDECK-API-VERSION: 1.2"

# curl opts to use a cookie jar, and follow redirects, showing only errors
CURLOPTS="-s -S -L -c $DIR/cookies -b $DIR/cookies"
CURL="curl $CURLOPTS"

XMLSTARLET=xml


# now submit req

params="$1"
shift

message="$*"

# get listing
$CURL --header "$VERSHEADER" -D $DIR/headers.out ${requrl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "FAIL: failed query request"
    exit 2
fi

#expect header code
grep "HTTP/1.1 ${ecode}" -q $DIR/headers.out 
if [ 0 != $? ] ; then
    errorMsg "FAIL: expected ${ecode} message, but was:"
    grep 'HTTP/1.1' $DIR/headers.out     
    exit 2
fi


exit 0