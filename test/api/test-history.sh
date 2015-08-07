#!/bin/bash

#Test api: /api/history output

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

proj="test"

# now submit req
runurl="${APIURL}/history"

echo "TEST: output from /api/history should be valid"

params="project=${proj}"

# get listing
docurl ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$(xmlsel "/result/events/@count" $DIR/curl.out)
[ "" == "$itemcount" ] && fail "expected events count"

EXPECTED="title summary user status @starttime @endtime node-summary/@succeeded node-summary/@failed node-summary/@total user project date-started date-ended"
for i in $EXPECTED ; do
    evalue=$($XMLSTARLET sel -T -t -m "/result/events/event[1]" -v "$i" $DIR/curl.out)
    [ "" == "$evalue" ] && fail "expected $i"
    evalue=""
done


echo "OK"

# use invalid dateTime format for "end" parameter

echo "TEST: /api/history using bad \"end\" date format parameter"
params="project=${proj}&end=asdf"

$SHELL $SRC_DIR/api-expect-error.sh "${runurl}" "${params}" "The parameter \"end\" did not have a valid time or dateTime format: asdf" || exit 2
echo "OK"


# use invalid dateTime format for "begin" parameter

echo "TEST: /api/history using bad \"begin\" date format parameter"
params="project=${proj}&begin=asdf"

$SHELL $SRC_DIR/api-expect-error.sh "${runurl}" "${params}" "The parameter \"begin\" did not have a valid time or dateTime format: asdf" || exit 2
echo "OK"

# use valid dateTime format for "end" parameter

echo "TEST: /api/history using valid \"end\" date format parameter"
params="project=${proj}&end=2011-02-04T21:38:02Z"

docurl ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2
echo "OK"
# use valid dateTime format for "begin" parameter

echo "TEST: /api/history using valid \"begin\" date format parameter"
params="project=${proj}&begin=2011-02-04T21:03:34Z"

docurl ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2
echo "OK"

rm $DIR/curl.out

