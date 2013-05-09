#!/bin/bash

# usage:
#  api-expect-code.sh <code> <URL> <params> <message>
# curls the URL with the params, and expects certain HTTP response code

errorMsg() {
   echo "$*" 1>&2
}

SRC_DIR=$(cd `dirname $0` && pwd)
DIR=${TMP_DIR:-$SRC_DIR}

ecode="$1"
shift

requrl="$1"
shift


# now submit req

params="$1"
shift

message="$*"

# get listing
if [ -n "$RDAUTH" ] ; then
    curl -L -s -S -H "X-RunDeck-Auth-Token: $RDAUTH" -D $DIR/headers.out ${requrl}?${params} > $DIR/curl.out
else
    curl -L -s -S -c $DIR/cookies -b $DIR/cookies -D $DIR/headers.out ${requrl}?${params} > $DIR/curl.out
fi
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
