#!/bin/bash

#test api version in header

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

# now submit req
runurl="${APIURL}/projects"

echo "TEST: API Version in header..."

# get listing
$CURL --header "X-RUNDECK-API-VERSION: 1.2" ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

sh $DIR/api-test-success.sh $DIR/curl.out || exit 2
echo "OK"

rm $DIR/curl.out

