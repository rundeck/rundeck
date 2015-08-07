#!/bin/bash

#test aborting execution from /api/execution/{id}/abort

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

####
# Setup: create simple adhoc command execution to provide execution ID.
####

runurl="${APIURL}/run/command"
proj="test"
params="project=${proj}&exec=echo+testing+execution+abort+api%3Bsleep+120"

# get listing
docurl -X POST ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#select id

execid=$(xmlsel "/result/execution/@id" $DIR/curl.out)

[ -z "$execid" ]  && fail "expected execution id"

####
# Test: get execution info
####

# now submit req
runurl="${APIURL}/execution/${execid}"

echo "TEST: /api/execution/${execid} ..."

params=""

# get listing
docurl ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$(xmlsel "/result/executions/@count" $DIR/curl.out)
assert "1" "$itemcount" "execution count should be 1"

assert "running" $(xmlsel "/result/executions/execution/@status" $DIR/curl.out) "execution was not running"

echo "OK"


####
# test /abort
####

runurl="${APIURL}/execution/${execid}/abort"

echo "TEST: /api/execution/${execid}/abort (json)..."

params=""

# pause
sleep 4

# get listing
docurl -D $DIR/headers.out -H 'Accept: application/json' -X POST ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"
assert_http_status 200 $DIR/headers.out

#Check projects list
assert_json_value "pending" ".abort.status" $DIR/curl.out
assert_json_value "$execid" ".execution.id" $DIR/curl.out
assert_json_value "running" ".execution.status" $DIR/curl.out

echo "OK"

# pause
sleep 4

####
# test result of /execution info
#### 

# now submit req
runurl="${APIURL}/execution/${execid}"

echo "TEST: /api/execution/${execid} ..."

params=""

# get listing
docurl ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$(xmlsel "/result/executions/@count" $DIR/curl.out)
assert "1" "$itemcount" "execution count should be 1"

assert "aborted" $(xmlsel "/result/executions/execution/@status" $DIR/curl.out) "execution should be aborted"
auser=$(xmlsel "/result/executions/execution/abortedby" $DIR/curl.out)

[ -z "$auser" ] && fail "execution did not have abortedby info"

echo "OK"

rm $DIR/curl.out

