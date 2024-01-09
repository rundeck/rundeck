#!/bin/bash

#test output from /api/executions/running

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh


# now submit req

proj=$2
if [ "" == "$2" ] ; then
    proj="test"
fi

echo "TEST: /api/executions/running for project ${proj}..."

runurl="${APIURL}/project/${proj}/executions/running"

# get listing
docurl ${runurl} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#Check projects list
itemcount=$(jq -r ".executions | length" $DIR/curl.out)
echo "$itemcount executions"
if [ "" == "$itemcount" ] ; then
    errorMsg "FAIL: executions count was not valid"
    exit 2
fi
echo "OK"




#rm $DIR/curl.out

