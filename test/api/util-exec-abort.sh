#!/bin/bash

#Usage:
#    util-exec-abort.sh <URL> <id>
#   Abort a running execution by ID.

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

execid=$1
shift
[ -z "$execid" ] && errorMsg "ID not specified" && exit 2

# now submit req
runurl="${APIURL}/execution/${execid}/abort"

echo "# Abort execution: ${execid}"

# get listing
docurl ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

xmlsel "/result/success/message" -n $DIR/curl.out
$XMLSTARLET sel -T -t -o "Abort status: " -v "/result/abort/@status" -n  $DIR/curl.out
$XMLSTARLET sel -T -t -o "Execution status: " -v "/result/abort/execution/@status" -n  $DIR/curl.out

rm $DIR/curl.out

