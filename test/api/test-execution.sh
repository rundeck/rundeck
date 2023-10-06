#!/bin/bash

#test output from /api/execution/{id}

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

####
# Setup: create simple adhoc command execution to provide execution ID.
####


proj="test"
runurl="${APIURL}/project/${proj}/run/command"
params="exec=echo+testing+execution+api"

# get listing
docurl -X POST ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#select id
execid=$(jq -r ".execution.id" < $DIR/curl.out)

if [ -z "$execid" ] ; then
    errorMsg "FAIL: expected execution id"
    exit 2
fi


####
# Test:
####

# now submit req
runurl="${APIURL}/execution/${execid}"
params=""

echo "TEST: ${runurl}?${params} (json) ..."

# get listing
docurl -D $DIR/headers.out -H 'accept: application/json' ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

assert_http_status 200 $DIR/headers.out

#Check projects list
assert_json_value "$execid" ".id" $DIR/curl.out
assert_json_not_null  ".href" $DIR/curl.out
assert_json_not_null  ".permalink" $DIR/curl.out
assert_json_not_null  ".status" $DIR/curl.out
assert_json_not_null  ".project" $DIR/curl.out
assert_json_not_null  ".user" $DIR/curl.out

echo "OK"


api_waitfor_execution $execid || {
  errorMsg "Failed to wait for execution $execid to finish"
  exit 2
}


#rm $DIR/curl.out

