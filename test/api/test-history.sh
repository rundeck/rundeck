#!/bin/bash

#Test api: /api/history output

errorMsg() {
   echo "$*" 1>&2
}

DIR=$(cd `dirname $0` && pwd)

# accept url argument on commandline, if '-' use default
url="$1"
if [ "-" == "$1" ] ; then
    url='http://localhost:4440'
fi
shift

proj="test"

apiurl="${url}/api"
VERSHEADER="X-RUNDECK-API-VERSION: 1.2"

# curl opts to use a cookie jar, and follow redirects, showing only errors
CURLOPTS="-s -S -L -c $DIR/cookies -b $DIR/cookies"
CURL="curl $CURLOPTS"

XMLSTARLET=xml

# now submit req
runurl="${apiurl}/history"

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

