#!/bin/bash

#test aborting execution from /api/execution/{id}/abort

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

####
# Setup: create simple adhoc command execution to provide execution ID.
####

runurl="${APIURL}/run/command"
proj="test"
params="project=${proj}&exec=echo+testing+execution+api%3Bsleep+120"

# get listing
$CURL --header "$VERSHEADER" ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

sh $DIR/api-test-success.sh $DIR/curl.out || exit 2

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

echo "TEST: /api/execution/${execid} ..."

params=""

# get listing
$CURL --header "$VERSHEADER" ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

sh $DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/executions/@count" $DIR/curl.out)
assert "1" "$itemcount" "execution count should be 1"

assert "running" $($XMLSTARLET sel -T -t -v "/result/executions/execution/@status" $DIR/curl.out) "execution was not running"

echo "OK"


####
# test /abort
####

runurl="${APIURL}/execution/${execid}/abort"

echo "TEST: /api/execution/${execid}/abort ..."

params=""

# pause
sleep 4

# get listing
$CURL --header "$VERSHEADER" ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

sh $DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
astatus=$($XMLSTARLET sel -T -t -v "/result/abort/@status" $DIR/curl.out)
aexecid=$($XMLSTARLET sel -T -t -v "/result/abort/execution/@id" $DIR/curl.out)
aexecstatus=$($XMLSTARLET sel -T -t -v "/result/abort/execution/@status" $DIR/curl.out)

assert "pending" "$astatus" "Abort status should be pending"
assert "$execid" "$aexecid" "Wrong execution id in abort status"
assert "running" "$aexecstatus" "Wrong execution status in abort status"

echo "OK"


rm $DIR/curl.out

