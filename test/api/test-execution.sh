#!/bin/bash

#test output from /api/execution/{id}

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

####
# Setup: create simple adhoc command execution to provide execution ID.
####

runurl="${APIURL}/run/command"
proj="test"
params="project=${proj}&exec=echo+testing+execution+api"

# get listing
docurl -X POST ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#select id

execid=$($XMLSTARLET sel -T -t -v "/result/execution/@id" $DIR/curl.out)

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


echo "TEST: ${runurl}?${params} ..."

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/executions/@count" $DIR/curl.out)
assert "1" "$itemcount" "execution count should be 1"
assert_xml_value "$execid" "/result/executions/execution/@id" $DIR/curl.out
assert_xml_notblank "/result/executions/execution/@href" $DIR/curl.out
assert_xml_notblank "/result/executions/execution/@permalink" $DIR/curl.out
assert_xml_notblank "/result/executions/execution/@status" $DIR/curl.out
assert_xml_notblank "/result/executions/execution/@project" $DIR/curl.out
assert_xml_notblank "/result/executions/execution/user" $DIR/curl.out

echo "OK"

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




#rm $DIR/curl.out

