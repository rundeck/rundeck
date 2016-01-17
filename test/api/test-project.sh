#!/bin/bash

#test result of /project/name metadata result

# use api V10
API_VERSION=10


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

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/projects/@count" $DIR/curl.out)
if [ "1" != "$itemcount" ] ; then
    errorMsg "FAIL: expected result of 1"
    exit 2
fi

echo "OK"

rm $DIR/curl.out

