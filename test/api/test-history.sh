#!/bin/bash

#Test api: /api/history output

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

proj="test"

# now submit req
runurl="${APIURL}/project/${proj}/history"

echo "TEST: output from /api/project/NAME/history should be valid"


# get listing
docurl ${runurl} > $DIR/curl.out || fail "failed request: ${runurl}"

#Check projects list
itemcount=$(jq -r ".events|length" $DIR/curl.out)
[ "" == "$itemcount" ] && fail "expected events count"

EXPECTED="title summary status starttime endtime \"node-summary\".succeeded \"node-summary\".failed \"node-summary\".total user project \"date-started\" \"date-ended\""
for i in $EXPECTED ; do
    evalue=$(jq -r ".events[0].$i" < $DIR/curl.out)
    [ "" == "$evalue" ] && fail "expected $i"
    evalue=""
done


echo "OK"

# use invalid dateTime format for "end" parameter

echo "TEST: /api/history using bad \"end\" date format parameter"
params="end=asdf"

$SHELL $SRC_DIR/api-expect-error.sh "${runurl}" "${params}" "The parameter \"end\" did not have a valid time or dateTime format: asdf" || exit 2
echo "OK"


# use invalid dateTime format for "begin" parameter

echo "TEST: /api/history using bad \"begin\" date format parameter"
params="begin=asdf"

$SHELL $SRC_DIR/api-expect-error.sh "${runurl}" "${params}" "The parameter \"begin\" did not have a valid time or dateTime format: asdf" || exit 2
echo "OK"

# use valid dateTime format for "end" parameter

echo "TEST: /api/history using valid \"end\" date format parameter"
params="end=2011-02-04T21:38:02Z"

docurl ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2
echo "OK"
# use valid dateTime format for "begin" parameter

echo "TEST: /api/history using valid \"begin\" date format parameter"
params="begin=2011-02-04T21:03:34Z"

docurl ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2
echo "OK"

rm $DIR/curl.out

