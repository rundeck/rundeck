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


#Check projects list
assert_json_value "$execid" ".id" $DIR/curl.out
assert_json_value "$expectstatus" ".status" $DIR/curl.out

exit 0
