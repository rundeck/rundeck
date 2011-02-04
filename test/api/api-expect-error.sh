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

message="$*"

# get listing
$CURL --header "$VERSHEADER" ${requrl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "FAIL: failed query request"
    exit 2
fi

#test curl.out for valid xml
$XMLSTARLET val -w $DIR/curl.out > /dev/null 2>&1
if [ 0 != $? ] ; then
    errorMsg "FAIL: Response was not valid xml"
    exit 2
fi

#test for expected /joblist element
$XMLSTARLET el $DIR/curl.out | grep -e '^result' -q
if [ 0 != $? ] ; then
    errorMsg "FAIL: Response did not contain expected result"
    exit 2
fi

#If <result error="true"> then an error occured.
waserror=$($XMLSTARLET sel -T -t -v "/result/@error" $DIR/curl.out)
if [ "true" != "$waserror" ] ; then
    errorMsg "FAIL: expected error result: ${message}"
    exit 2
fi
if [ "" != "${message}" ] ; then 
    errmsg=$($XMLSTARLET sel -T -t -v "/result/error/message" $DIR/curl.out)
    if [ "${errmsg}" != "${message}" ] ; then
        errorMsg "FAIL: wrong error message: ${errmsg}, expected ${message}"
        exit 2
    fi
fi

exit 0