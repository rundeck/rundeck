#!/bin/bash

# usage:
#  api-expect-error.sh <URL> <params> <message>
# curls the URL with the params, and expects result error="true", with result message if specified
DIR=$(cd `dirname $0` && pwd)

errorMsg() {
   echo "$*" 1>&2
}


requrl="$1"
shift

params="$1"
shift

# get listing
if [ -n "$RDAUTH" ] ; then
    curl -L -s -S -H "X-RunDeck-Auth-Token: $RDAUTH" -D $DIR/headers.out $CURL_REQ_OPTS ${requrl}?${params} > $DIR/curl.out
else
    curl -L -s -S -c $DIR/cookies -b $DIR/cookies -D $DIR/headers.out $CURL_REQ_OPTS ${requrl}?${params} > $DIR/curl.out
fi
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
