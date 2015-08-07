#!/bin/bash

#Usage: 
#    util-run.sh <URL> <project> commands...

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

proj=$1
shift

execargs="$1"
shift
qparams="$*"
# now submit req
runurl="${APIURL}/run/command"

echo "# Run command: ${execargs}"

params="project=${proj}&${qparams}"

# get listing
docurl --data-urlencode "exec=${execargs}" ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

# job list query doesn't wrap result in common result wrapper
#If <result error="true"> then an error occured.
xmlsel "/result/success/message" -n $DIR/curl.out
$XMLSTARLET sel -T -t -o "Execution started with ID: " -v "/result/execution/@id" -n  $DIR/curl.out

rm $DIR/curl.out

