#!/bin/bash

#test output from /api/report/create

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

runurl="${APIURL}/report/create"
proj="test"
xtime=$(date "+%F-%T")

#test missing parameters - project

echo "TEST: missing project parameter"

reportparams="title=test&status=succeeded&nodesuccesscount=1&nodefailcount=0&summary=test+is+ok+${xtime}"
params="${reportparams}"

# get listing
sh $DIR/api-expect-error.sh "${runurl}" "${params}" "parameter \"project\" is required" || exit 2
echo "OK"


#test missing parameters - status

echo "TEST: missing status parameter"

reportparams="title=test&x=succeeded&nodesuccesscount=1&nodefailcount=0&summary=test+is+ok+${xtime}"
params="project=${proj}&${reportparams}"

# get listing
sh $DIR/api-expect-error.sh "${runurl}" "${params}" "parameter \"status\" is required" || exit 2
echo "OK"


#test missing parameters - title

echo "TEST: missing title parameter"

reportparams="x=test&status=succeeded&nodesuccesscount=1&nodefailcount=0&summary=test+is+ok+${xtime}"
params="project=${proj}&${reportparams}"

# get listing
sh $DIR/api-expect-error.sh "${runurl}" "${params}" "parameter \"title\" is required" || exit 2
echo "OK"

#test missing parameters - nodesuccesscount

echo "TEST: missing nodesuccesscount parameter"

reportparams="title=test&status=succeeded&x=1&nodefailcount=0&summary=test+is+ok+${xtime}"
params="project=${proj}&${reportparams}"

# get listing
sh $DIR/api-expect-error.sh "${runurl}" "${params}" "parameter \"nodesuccesscount\" is required" || exit 2
echo "OK"

#test missing parameters - nodefailcount

echo "TEST: missing nodefailcount parameter"

reportparams="title=test&status=succeeded&nodesuccesscount=1&x=0&summary=test+is+ok+${xtime}"
params="project=${proj}&${reportparams}"

# get listing
sh $DIR/api-expect-error.sh "${runurl}" "${params}" "parameter \"nodefailcount\" is required" || exit 2
echo "OK"


#test missing parameters - summary

echo "TEST: missing summary parameter"

reportparams="title=test&status=succeeded&nodesuccesscount=1&nodefailcount=0&x=test+is+ok+${xtime}"
params="project=${proj}&${reportparams}"

# get listing
sh $DIR/api-expect-error.sh "${runurl}" "${params}" "parameter \"summary\" is required" || exit 2
echo "OK"


#test wrong parameter - status value

echo "TEST: wrong status value"

reportparams="title=test&status=blah&nodesuccesscount=1&nodefailcount=0&summary=test+is+ok+${xtime}"
params="project=${proj}&${reportparams}"

# get listing
sh $DIR/api-expect-error.sh "${runurl}" "${params}" "the value \"blah\" for parameter \"status\" was invalid. It must be in the list: [succeeded, aborted, failed]" || exit 2
echo "OK"


#test wrong parameter - end value

echo "TEST: wrong end value"

reportparams="title=test&status=succeeded&nodesuccesscount=1&nodefailcount=0&summary=test+is+ok+${xtime}&end=not-a-time"
params="project=${proj}&${reportparams}"

# get listing
sh $DIR/api-expect-error.sh "${runurl}" "${params}" "The parameter \"end\" did not have a valid time or dateTime format: not-a-time" || exit 2
echo "OK"

#test wrong parameter - start value

echo "TEST: wrong start value"

reportparams="title=test&status=succeeded&nodesuccesscount=1&nodefailcount=0&summary=test+is+ok+${xtime}&start=not-a-time"
params="project=${proj}&${reportparams}"

# get listing
sh $DIR/api-expect-error.sh "${runurl}" "${params}" "The parameter \"start\" did not have a valid time or dateTime format: not-a-time" || exit 2
echo "OK"


####
# test success
####

echo "TEST: Report execution status succeeded..."

reportparams="title=test&status=succeeded&nodesuccesscount=1&nodefailcount=0&summary=test+is+ok+${xtime}"

params="project=${proj}&${reportparams}"

# get listing
$CURL  ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"
sh $DIR/api-test-success.sh $DIR/curl.out || exit 2

# get history and look for test

params="project=${proj}&max=1"

$CURL  "${APIURL}/history/?${params}" > $DIR/curl.out || fail "failed request: ${runurl}"
sh $DIR/api-test-success.sh $DIR/curl.out || exit 2

assert "1" $(xmlsel "/result/events/@count" $DIR/curl.out)
# assert "1298512842793" $(xmlsel "/result/events/event/@starttime" $DIR/curl.out)
# assert "1298512842794" $(xmlsel "/result/events/event/@endtime" $DIR/curl.out)
assert "test" $(xmlsel "/result/events/event/title" $DIR/curl.out)
assert "succeeded" $(xmlsel "/result/events/event/status" $DIR/curl.out)
assert "test is ok ${xtime}" "$(xmlsel /result/events/event/summary $DIR/curl.out)"
assert "test" $(xmlsel "/result/events/event/project" $DIR/curl.out)
assert "1" $(xmlsel "/result/events/event/node-summary/@succeeded" $DIR/curl.out)
assert "0" $(xmlsel "/result/events/event/node-summary/@failed" $DIR/curl.out)
assert "1" $(xmlsel "/result/events/event/node-summary/@total" $DIR/curl.out)
assert "admin" $(xmlsel "/result/events/event/user" $DIR/curl.out)


echo "OK"


####
# test failure
####

echo "TEST: Report execution status failed..."

reportparams="title=test-failed&status=failed&nodesuccesscount=1&nodefailcount=3&summary=test-failed+is+ok+${xtime}"

params="project=${proj}&${reportparams}"

# get listing
$CURL  ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"
sh $DIR/api-test-success.sh $DIR/curl.out || exit 2

# get history and look for test

runurl="${APIURL}/history"

params="project=${proj}&max=1"

$CURL  ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"
sh $DIR/api-test-success.sh $DIR/curl.out || exit 2

assert "1" $(xmlsel "/result/events/@count" $DIR/curl.out)
# assert "1298512842793" $(xmlsel "/result/events/event/@starttime" $DIR/curl.out)
# assert "1298512842794" $(xmlsel "/result/events/event/@endtime" $DIR/curl.out)
assert "test-failed" $(xmlsel "/result/events/event/title" $DIR/curl.out)
assert "failed" $(xmlsel "/result/events/event/status" $DIR/curl.out)
assert "test-failed is ok ${xtime}" "$(xmlsel /result/events/event/summary $DIR/curl.out)"
assert "test" $(xmlsel "/result/events/event/project" $DIR/curl.out)
assert "1" $(xmlsel "/result/events/event/node-summary/@succeeded" $DIR/curl.out)
assert "3" $(xmlsel "/result/events/event/node-summary/@failed" $DIR/curl.out)
assert "4" $(xmlsel "/result/events/event/node-summary/@total" $DIR/curl.out)
assert "admin" $(xmlsel "/result/events/event/user" $DIR/curl.out)


echo "OK"
rm $DIR/curl.out

