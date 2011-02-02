#!/bin/bash

#test output from /api/jobs/export

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
proj=$2
if [ "" == "$2" ] ; then
    proj="test"
fi

echo "TEST: export RunDeck Jobs in jobs.xml format"

params="project=${proj}"

# get listing
$CURL --header "$VERSHEADER" ${runurl}?${params} > $DIR/curl.out
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
$XMLSTARLET el $DIR/curl.out | grep -e '^joblist' -q
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response did not contain expected result"
    exit 2
fi

# job export doesn't wrap result in common result wrapper
#Check projects list
itemcount=$($XMLSTARLET sel -T -t -m "/joblist" -v "count(job)" $DIR/curl.out)
if [ "" == "$itemcount" ] ; then
    errMsg "Unexpected result"
    exit 2
else
    echo "OK"
fi



rm $DIR/curl.out

