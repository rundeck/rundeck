#!/bin/bash

# usage:
#  api-expect-exec-success.sh ID [message]
# tests an execution status is succeeded

execid="$1"
shift
expectstatus=${1:-succeeded}
shift

# arg to include.sh
set -- -

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh


# now submit req
runurl="${APIURL}/execution/${execid}"


params=""

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request ${runurl}?${params}"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || (echo "${runurl}?${params}"; exit 2)

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/executions/@count" $DIR/curl.out)
assert "1" "$itemcount" "execution count should be 1"
status=$($XMLSTARLET sel -T -t -v "//execution[@id=$execid]/@status" $DIR/curl.out)
assert "$expectstatus" "$status" "execution status should be succeeded"

exit 0