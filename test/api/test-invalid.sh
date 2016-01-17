#!/bin/bash

#test output from invalid api request path: /api/dnexist

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

# now submit req
runurl="${APIURL}/dnexist"

echo "TEST: api request to invalid api path"

params="project=test"

# get listing
docurl -D $DIR/headers.out ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-error.sh $DIR/curl.out || exit 2

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

