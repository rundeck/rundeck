#!/bin/bash

#test output from invalid api request path: /api/dnexist

errorMsg() {
   echo "$*" 1>&2
}

DIR=$(cd `dirname $0` && pwd)

# accept url argument on commandline, if '-' use default
url="$1"
if [ "-" == "$1" ] ; then
    url='http://localhost:4440/api'
fi
apiurl="${url}/api"
VERSHEADER="X-RUNDECK-API-VERSION: 1.2"

# curl opts to use a cookie jar, and follow redirects, showing only errors
CURLOPTS="-s -S -L -c $DIR/cookies -b $DIR/cookies"
CURL="curl $CURLOPTS"


XMLSTARLET=xml

# now submit req
runurl="${apiurl}/dnexist"

echo "TEST: api request to invalid api path"

params="project=test"

# get listing
$CURL --header "$VERSHEADER" -D $DIR/headers.out ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

sh $DIR/api-test-error.sh $DIR/curl.out || exit 2

#test result error message

errmsg=$($XMLSTARLET sel -T -t -v "/result/error/message" $DIR/curl.out)
substr=${errmsg#Invalid API Request:}
if [  "Invalid API Request:$substr" == "$errmsg" ] ; then
    echo "OK"
else
    errorMsg "TEST FAILED: Invalid API Request message expected: $errmsg"
    exit 2
fi

rm $DIR/curl.out
rm $DIR/headers.out

