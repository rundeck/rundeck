#!/bin/bash

#test aborting execution from /api/execution/{id}/abort

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

####
# Setup: create simple adhoc command execution to provide execution ID.
####


proj="test"
runurl="${APIURL}/project/${proj}/run/command"
params="exec=echo+testing+execution+abort+api%3Bsleep+120"

# get listing
docurl -X POST ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

#select id

execid=$(xmlsel "//execution/@id" $DIR/curl.out)

[ -z "$execid" ]  && fail "expected execution id"

####
# Test: get execution info
####
sleep 5

# now submit req
runurl="${APIURL}/execution/${execid}"

echo "TEST: /api/execution/${execid} ..."

params=""

# get listing
docurl ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

#Check projects list
itemcount=$(xmlsel "//executions/@count" $DIR/curl.out)
assert "1" "$itemcount" "execution count should be 1"

assert "running" $(xmlsel "//executions/execution/@status" $DIR/curl.out) "execution was not running"

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
docurl -X POST ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"


#Check projects list
astatus=$(xmlsel "//abort/@status" $DIR/curl.out)
aexecid=$(xmlsel "//abort/execution/@id" $DIR/curl.out)
aexecstatus=$(xmlsel "//abort/execution/@status" $DIR/curl.out)

assert "pending" "$astatus" "Abort status should be pending"
assert "$execid" "$aexecid" "Wrong execution id in abort status"
assert "running" "$aexecstatus" "Wrong execution status in abort status"

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


#Check projects list
itemcount=$(xmlsel "//executions/@count" $DIR/curl.out)
assert "1" "$itemcount" "execution count should be 1"

assert "aborted" $(xmlsel "//executions/execution/@status" $DIR/curl.out) "execution should be aborted"
auser=$(xmlsel "//executions/execution/abortedby" $DIR/curl.out)

[ -z "$auser" ] && fail "execution did not have abortedby info"

echo "OK"

rm $DIR/curl.out

