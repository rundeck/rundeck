#!/bin/bash

# TEST: /api/run/command action

errorMsg() {
   echo "$*" 1>&2
}

DIR=$(cd `dirname $0` && pwd)

proj="test"

# accept url argument on commandline, if '-' use default
url="$1"
if [ "-" == "$1" ] ; then
    url='http://localhost:4440'
fi
shift
apiurl="${url}/api"
VERSHEADER="X-RUNDECK-API-VERSION: 1.2"

# curl opts to use a cookie jar, and follow redirects, showing only errors
CURLOPTS="-s -S -L -c $DIR/cookies -b $DIR/cookies"
CURL="curl $CURLOPTS"

if [ ! -f $DIR/cookies ] ; then 
    # call rundecklogin.sh
    sh $DIR/rundecklogin.sh $url
fi

XMLSTARLET=xml

execargs="echo this is a test of /api/run/command"

# now submit req
runurl="${apiurl}/run/command"

echo "TEST: /api/run/command should fail with no project param"
sh $DIR/api-expect-error.sh "${runurl}" "project=" 'parameter "project" is required' && echo "OK" || exit 2


echo "TEST: /api/run/command should fail with no exec param"
params="project=${proj}"
sh $DIR/api-expect-error.sh "${runurl}" "${params}" 'parameter "exec" is required' && echo "OK" || exit 2

echo "TEST: /api/run/command should succeed and return execution id"
# make api request
$CURL --header "$VERSHEADER" --data-urlencode "exec=${execargs}" ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "FAIL: failed query request"
    exit 2
fi

sh $DIR/api-test-success.sh $DIR/curl.out || exit 2
execid=$($XMLSTARLET sel -T -t -o "Execution started with ID: " -v "/result/execution/@id" -n $DIR/curl.out)
if [ "" == "${execid}" ] ; then
    errorMsg "FAIL: expected execution id in result: ${execid}"
    exit 2
fi

echo "OK"

rm $DIR/curl.out