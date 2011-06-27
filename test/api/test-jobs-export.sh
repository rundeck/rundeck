#!/bin/bash

#test output from /api/jobs/export

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

# now submit req
runurl="${APIURL}/jobs/export"
proj=$2
if [ "" == "$2" ] ; then
    proj="test"
fi

echo "TEST: export RunDeck Jobs in jobs.xml format"

params="project=${proj}"

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
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

