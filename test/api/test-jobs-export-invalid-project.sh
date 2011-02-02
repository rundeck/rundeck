#!/bin/bash

#test output from /api/jobs/export with invalid project

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
runurl="${apiurl}/jobs/export"

echo "TEST: export RunDeck Jobs in jobs.xml format [invalid project parameter]"

params="project=DNEProject"

# get listing
$CURL --header "$VERSHEADER" -D $DIR/headers.out ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#test curl.out for valid xml
$XMLSTARLET val -w $DIR/curl.out > /dev/null 2>&1
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response was not valid xml"
    exit 2
fi

#test for expected /joblist element
$XMLSTARLET el $DIR/curl.out | grep -e '^result' -q
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response did not contain expected result"
    exit 2
fi

#test result error message
waserror=$($XMLSTARLET sel -T -t -v "/result/@error" $DIR/curl.out)
errmsg=$($XMLSTARLET sel -T -t -v "/result/error/message" $DIR/curl.out)
if [ "true" == "$waserror" -a "project does not exist: DNEProject" == "$errmsg" ] ; then
    echo "OK"
else
    errorMsg "TEST FAILED: nonexistent project message expected: $errmsg"
    exit 2
fi



rm $DIR/curl.out
rm $DIR/headers.out

