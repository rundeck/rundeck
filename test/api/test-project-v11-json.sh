#!/bin/bash

#test result of /project/name JSON format
#using API v11

# use api V11
API_VERSION=11

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh


JQ=`which jq`

if [ -z "$JQ" ] ; then
    errorMsg "FAIL: Can't test JSON format, install jq"
    exit 2
fi

# now submit req
proj="test"

runurl="${APIURL}/project/${proj}"

echo "TEST: /api/project/${proj}..."

# get listing
docurl -H Accept:application/json ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#Check projects list
name=$($JQ -r .name < $DIR/curl.out)
if [ "$proj" != "$name" ] ; then
    errorMsg "FAIL: expected .name value of $proj"
    exit 2
fi
url=$($JQ -r .url < $DIR/curl.out)
if [ -z "$url" ] ; then
    errorMsg "FAIL: expected /project/@url in result"
    exit 2
fi

echo "OK"

rm $DIR/curl.out

