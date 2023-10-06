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
assert_json_not_null ".execution.id" $DIR/curl.out
execid=$(jq -r ".execution.id" < $DIR/curl.out)

[ -z "$execid" ]  && fail "expected execution id"

####
# Test: get execution info
####

# now submit req
runurl="${APIURL}/execution/${execid}"

echo "TEST: /api/execution/${execid} ..."

params=""

sleep 5

# get listing
docurl ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

assert_json_value "$execid" ".id" $DIR/curl.out
assert_json_value "running" ".status" $DIR/curl.out

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

#Check projects list
assert_json_value "$execid" ".id" $DIR/curl.out
assert_json_value "aborted" ".status" $DIR/curl.out

auser=$(jq -r ".abortedby" < $DIR/curl.out)

[ -z "$auser" ] && fail "execution did not have abortedby info"

echo "OK"

rm $DIR/curl.out

