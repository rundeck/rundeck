#!/bin/bash

#test result of /project/name JSON format
#using API v11

# use api V11
API_VERSION=11

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

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
assert_json_value "$proj" .name $DIR/curl.out
assert_json_value "$CUR_APIURL/project/$proj" .url $DIR/curl.out


echo "OK"

rm $DIR/curl.out

