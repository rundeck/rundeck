#!/bin/bash

#test result of /project/name metadata result
#using API v14, no xml result wrapper

# use api V14
API_VERSION=14

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
#
assert_json_value $proj ".name" $DIR/curl.out
assert_json_value "$CUR_APIURL/project/$proj" ".url" $DIR/curl.out


echo "OK"

rm $DIR/curl.out

