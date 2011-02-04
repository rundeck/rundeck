#!/bin/bash

# usage:
#  api-expect-error.sh <URL> <params> <message>
# curls the URL with the params, and expects result error="true", with result message if specified

errorMsg() {
   echo "$*" 1>&2
}

DIR=$(cd `dirname $0` && pwd)

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


# get listing
$CURL -D $DIR/headers.out $CURL_REQ_OPTS --header "$VERSHEADER" ${requrl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "FAIL: failed query request"
    exit 2
fi


grep "HTTP/1.1 200" -q $DIR/headers.out
okheader=$?
grep "HTTP/1.1 302" -q $DIR/headers.out
ok2header=$?
if [ 0 != $okheader -a 0 != $ok2header ] ; then
    errorMsg "FAIL: Response was not 200 OK or 302:"
    grep 'HTTP/1.1' $DIR/headers.out
    exit 2
fi
rm $DIR/headers.out

sh $DIR/api-test-error.sh $DIR/curl.out $*
