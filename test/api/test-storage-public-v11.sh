#!/bin/bash

#test POST /api/11/storage/keys
#using API v11, no xml result wrapper

# use api V11
API_VERSION=11
API_XML_NO_WRAPPER=true

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

echo "POST /api/11/storage/keys/key1.public"

##
# Post public key
##
cat > $DIR/key1_public.post <<END
fake data
END

runurl="${APIURL}/storage/keys/key1.public"
testurl="${CUR_APIURL}/storage/keys/key1.public"

# post
docurl -X POST -D $DIR/headers.out --data-binary @$DIR/key1_public.post -H Accept:application/xml \
     -H Content-Type:application/pgp-keys ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed POST request"
    exit 2
fi
assert_http_status 201 $DIR/headers.out

echo "OK"

##
# get public key should result in metadata
##

echo "GET $runurl (xml)"

# post
docurl -D $DIR/headers.out -H Accept:application/xml  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed POST request"
    exit 2
fi
assert_http_status 200 $DIR/headers.out

API_XML_NO_WRAPPER=true $SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check result

assert_xml_value "keys/key1.public" "/resource/@path" $DIR/curl.out
assert_xml_value "file" "/resource/@type" $DIR/curl.out
assert_xml_value "key1.public" "/resource/@name" $DIR/curl.out
assert_xml_value "$testurl" "/resource/@url" $DIR/curl.out
assert_xml_value "application/pgp-keys" "/resource/resource-meta/Rundeck-content-type" $DIR/curl.out
assert_xml_value "10" "/resource/resource-meta/Rundeck-content-size" $DIR/curl.out
assert_xml_value "" "/resource/resource-meta/Rundeck-content-mask" $DIR/curl.out
assert_xml_value "public" "/resource/resource-meta/Rundeck-key-type" $DIR/curl.out

echo "OK"

echo "GET $runurl (json)"

docurl -D $DIR/headers.out -H Accept:application/json  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed POST request"
    exit 2
fi
assert_http_status 200 $DIR/headers.out

#Check result

assert_json_value "keys/key1.public" ".path" $DIR/curl.out
assert_json_value "file" ".type" $DIR/curl.out
assert_json_value "key1.public" ".name" $DIR/curl.out
assert_json_value "$testurl" ".url" $DIR/curl.out
assert_json_value "application/pgp-keys" .meta['"Rundeck-content-type"'] $DIR/curl.out
assert_json_value "10" ".meta[\"Rundeck-content-size\"]" $DIR/curl.out
assert_json_null ".meta[\"Rundeck-content-mask\"]" $DIR/curl.out
assert_json_value "public" ".meta[\"Rundeck-key-type\"]" $DIR/curl.out

echo "OK"


echo "GET $runurl (accept public key)"

# post
docurl -D $DIR/headers.out -H Accept:application/pgp-keys  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed POST request"
    exit 2
fi
assert_http_status 200 $DIR/headers.out

diff -q $DIR/curl.out $DIR/key1_public.post

if [ 0 != $? ] ; then
    errorMsg "ERROR: retrieved data is not the same as expected"
    exit 2
fi

echo "OK"


runurl="${APIURL}/storage/keys/"

echo "GET $runurl (list)"
# post
docurl -D $DIR/headers.out -H Accept:application/xml  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed POST request"
    exit 2
fi
assert_http_status 200 $DIR/headers.out

API_XML_NO_WRAPPER=true $SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check result


assert_xml_value "keys" "/resource/@path" $DIR/curl.out
assert_xml_value "directory" "/resource/@type" $DIR/curl.out
assert_xml_value "${CUR_APIURL}/storage/keys" "/resource/@url" $DIR/curl.out
assert_xml_value "1" "/resource/contents/@count" $DIR/curl.out

assert_xml_value "keys/key1.public" "/resource/contents/resource[1]/@path" $DIR/curl.out

echo "OK"



runurl="${APIURL}/storage/keys/key1.public"

echo "DELETE $runurl"
# post
docurl -X DELETE -D $DIR/headers.out -H Accept:application/xml  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed POST request"
    exit 2
fi
assert_http_status 204 $DIR/headers.out

echo "OK"

rm $DIR/key1_public.post
rm $DIR/curl.out

