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
$CURL --header "$VERSHEADER" ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

sh $DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/events/@count" $DIR/curl.out)
if [ "" == "$itemcount" ] ; then
    errorMsg "FAIL: expected events count"
    exit 2
fi

echo "OK"

# use invalid dateTime format for "end" parameter

echo "TEST: /api/history using bad \"end\" date format parameter"
params="project=${proj}&end=asdf"

sh $DIR/api-expect-error.sh "${runurl}" "${params}" "The parameter \"end\" did not have a valid time or dateTime format: asdf" || exit 2
echo "OK"


# use invalid dateTime format for "begin" parameter

echo "TEST: /api/history using bad \"begin\" date format parameter"
params="project=${proj}&begin=asdf"

sh $DIR/api-expect-error.sh "${runurl}" "${params}" "The parameter \"begin\" did not have a valid time or dateTime format: asdf" || exit 2
echo "OK"

# use valid dateTime format for "end" parameter

echo "TEST: /api/history using valid \"end\" date format parameter"
params="project=${proj}&end=2011-02-04T21:38:02Z"

$CURL --header "$VERSHEADER" ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

sh $DIR/api-test-success.sh $DIR/curl.out || exit 2
echo "OK"
# use valid dateTime format for "begin" parameter

echo "TEST: /api/history using valid \"begin\" date format parameter"
params="project=${proj}&begin=2011-02-04T21:03:34Z"

$CURL --header "$VERSHEADER" ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

sh $DIR/api-test-success.sh $DIR/curl.out || exit 2
echo "OK"

#rm $DIR/curl.out

