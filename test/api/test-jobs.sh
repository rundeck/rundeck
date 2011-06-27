#!/bin/bash

#test output from /api/jobs

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

# now submit req
runurl="${APIURL}/jobs"

proj="test"

echo "Listing RunDeck Jobs for project ${proj}..."

params="project=${proj}"

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

sh $DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/jobs/@count" $DIR/curl.out)

if [ "" == "$itemcount" ] ; then
    errorMsg "Wrong count:"
    exit 2
fi
echo "OK"




rm $DIR/curl.out

