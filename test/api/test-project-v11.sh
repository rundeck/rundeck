#!/bin/bash

#test result of /project/name metadata result
#using API v11, no xml result wrapper

# use api V11
API_VERSION=11
API_XML_NO_WRAPPER=true

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

# now submit req
proj="test"

runurl="${APIURL}/project/${proj}"

echo "TEST: /api/project/${proj}..."

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

API_XML_NO_WRAPPER=true sh $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
name=$($XMLSTARLET sel -T -t -v "/project/name" $DIR/curl.out)
if [ "$proj" != "$name" ] ; then
    errorMsg "FAIL: expected /project/name value of $proj"
    exit 2
fi
url=$($XMLSTARLET sel -T -t -v "/project/@url" $DIR/curl.out)
if [ -z "$url" ] ; then
    errorMsg "FAIL: expected /project/@url in result"
    exit 2
fi

echo "OK"

rm $DIR/curl.out

