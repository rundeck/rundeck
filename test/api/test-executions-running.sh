#!/bin/bash

#test output from /api/executions/running

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh


# now submit req
runurl="${APIURL}/executions/running"
proj=$2
if [ "" == "$2" ] ; then
    proj="test"
fi

echo "TEST: /api/executions/running for project ${proj}..."

params="project=${proj}"

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/executions/@count" $DIR/curl.out)
echo "$itemcount executions"
if [ "" == "$itemcount" ] ; then
    errorMsg "FAIL: executions count was not valid"
    exit 2
fi
echo "OK"




#rm $DIR/curl.out

