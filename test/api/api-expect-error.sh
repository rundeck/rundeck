#!/bin/bash

# usage:
#  api-expect-error.sh <URL> <params> <message>
# curls the URL with the params, and expects result error="true", with result message if specified
SRC_DIR=$(cd `dirname $0` && pwd)
DIR=${TMP_DIR:-$SRC_DIR}

errorMsg() {
   echo "$*" 1>&2
}


requrl="$1"
shift

params="$1"
shift

message="$1"
shift

code="${1:-400}"
shift

set -- $requrl

source $SRC_DIR/include.sh

# get listing
docurl -D $DIR/headers.out $CURL_REQ_OPTS ${requrl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "FAIL: failed query request"
    exit 2
fi


grep "HTTP/1.1 $code" -q $DIR/headers.out
okheader=$?
if [ 0 != $okheader ] ; then
    errorMsg "FAIL: Response was not $code"
    grep 'HTTP/1.1' $DIR/headers.out
    exit 2
fi
rm $DIR/headers.out

$SHELL $SRC_DIR/api-test-error.sh $DIR/curl.out "$message"
