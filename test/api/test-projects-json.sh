#!/bin/bash

#test /api/projects
#using API v14, JSON format

# use api V14
API_VERSION=14

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh


JQ=`which jq`

if [ -z "$JQ" ] ; then
    errorMsg "FAIL: Can't test JSON format, install jq"
    exit 2
fi

# now submit req
runurl="${APIURL}/projects"

echo "TEST: /api/14/projects "

# get listing
docurl -H Accept:application/json ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#Check projects list
itemcount=$($JQ length < $DIR/curl.out)
if [ $itemcount  -lt 1  ] ; then
    errorMsg "Expected at least one project object in json array"
    exit 2
fi
if [ "" == "$itemcount"  ] ; then
    errorMsg "Wrong count"
    exit 2
fi

echo "OK"



rm $DIR/curl.out

