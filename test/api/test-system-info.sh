#!/bin/bash

#test output from /api/system/info

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh



####
# Test:
####

runurl="${APIURL}/system/info"
params="format=json"

echo "TEST: ${runurl}?${params} ..."


# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

assert_json_value "${API_VERSION}" ".system.rundeck.apiversion" $DIR/curl.out || exit 2


echo "OK"

runurl="${APIURL}/system/info"
params=""

echo "TEST: ${runurl}?${params} (accept:json)..."


# get listing
docurl -H 'Accept:application/json' ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

assert_json_value "${API_VERSION}" ".system.rundeck.apiversion" $DIR/curl.out || exit 2


echo "OK"


#rm $DIR/curl.out

