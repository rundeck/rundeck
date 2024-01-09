#!/bin/bash

#test POST /api/14/storage/keys
#using API v14, no xml result wrapper

# use api V14
API_VERSION=14
API_XML_NO_WRAPPER=true

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh
BASE_PATH="/test"

echo "POST /api/14/storage/keys${BASE_PATH}/key1.private"

##
# Post private key
##
cat > $DIR/key1_private.post <<END
fake data
END

runurl="${APIURL}/storage/keys${BASE_PATH}/key1.private"
testurl="${CUR_APIURL}/storage/keys${BASE_PATH}/key1.private"

# post
docurl -X POST -D $DIR/headers.out --data-binary @$DIR/key1_private.post -H Accept:application/json \
     -H Content-Type:application/octet-stream ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed POST request"
    exit 2
fi
assert_http_status 201 $DIR/headers.out

echo "OK"

##
# get private key should result in metadata
##
echo "GET $runurl (json)"

docurl -D $DIR/headers.out -H Accept:application/json  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed POST request"
    exit 2
fi
assert_http_status 200 $DIR/headers.out

#Check result

assert_json_value "keys${BASE_PATH}/key1.private" ".path" $DIR/curl.out
assert_json_value "file" ".type" $DIR/curl.out
assert_json_value "key1.private" ".name" $DIR/curl.out
assert_json_value "$testurl" ".url" $DIR/curl.out
assert_json_value "application/octet-stream" .meta['"Rundeck-content-type"'] $DIR/curl.out
assert_json_null  ".meta[\"Rundeck-content-size\"]" $DIR/curl.out
assert_json_value "content" ".meta[\"Rundeck-content-mask\"]" $DIR/curl.out
assert_json_value "private" ".meta[\"Rundeck-key-type\"]" $DIR/curl.out

echo "OK"


echo "GET $runurl (accept private key)"

# post
docurl -D $DIR/headers.out -H Accept:application/octet-stream  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed POST request"
    exit 2
fi
assert_http_status 403 $DIR/headers.out


echo "OK"


runurl="${APIURL}/storage/keys${BASE_PATH}/"

echo "GET $runurl (list)"
# post
docurl -D $DIR/headers.out -H Accept:application/json  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed POST request"
    exit 2
fi
assert_http_status 200 $DIR/headers.out

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check result


assert_json_value "keys${BASE_PATH}" ".path" $DIR/curl.out
assert_json_value "directory" ".type" $DIR/curl.out
assert_json_value "${CUR_APIURL}/storage/keys${BASE_PATH}" ".url" $DIR/curl.out
assert_json_value "1" ".resources|length" $DIR/curl.out

assert_json_value "keys${BASE_PATH}/key1.private" ".resources[0].path" $DIR/curl.out

echo "OK"



runurl="${APIURL}/storage/keys${BASE_PATH}/key1.private"

echo "DELETE $runurl"
# post
docurl -X DELETE -D $DIR/headers.out -H Accept:application/json  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed POST request"
    exit 2
fi
assert_http_status 204 $DIR/headers.out

echo "OK"

rm $DIR/key1_private.post
rm $DIR/curl.out

