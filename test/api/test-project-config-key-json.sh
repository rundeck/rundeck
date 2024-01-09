#!/bin/bash

#test GET /api/14/project/name/config/key
#using API v14, json format

# use api V14
API_VERSION=14
API_XML_NO_WRAPPER=true

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

# now submit req
runurl="${APIURL}/projects"
test_proj="APIConfigTest2"


create_project "$test_proj" '{"test.property":"test value", "test.property2":"test value2"}'
#Check result

assert_json_value $test_proj '.name' $DIR/curl.out
assert_json_value "test value" ".config.\"test.property\"" $DIR/curl.out
assert_json_value "test value2" ".config.\"test.property2\"" $DIR/curl.out
assert_json_null ".config.\"test.property3\"" $DIR/curl.out


runurl="${APIURL}/project/$test_proj/config/test.property"

echo "TEST: GET $runurl"

# post
docurl -H 'Accept:application/json' ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed GET request"
    exit 2
fi

assert_json_value 'test value' '.value' $DIR/curl.out

echo "OK"

runurl="${APIURL}/project/$test_proj/config/test.property2"

echo "TEST: GET $runurl"

# post
docurl -H 'Accept:application/json' ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed GET request"
    exit 2
fi

assert_json_value 'test value2' '.value' $DIR/curl.out

echo "OK"

runurl="${APIURL}/project/$test_proj/config/test.property"

echo "TEST: PUT $runurl"

value="{\"key\":\"test.property\",\"value\":\"Btest value\"}"
# post
docurl -X PUT --data-binary "${value}" -H 'Content-Type:application/json' ${runurl} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed GET request"
    exit 2
fi

assert_json_value 'Btest value' '.value' $DIR/curl.out

echo "OK"

runurl="${APIURL}/project/$test_proj/config/test.property2"

echo "TEST: PUT $runurl"

value="{\"key\":\"test.property2\", \"value\":\"Btest value2\"}"
# post
docurl -X PUT --data-binary "${value}" -H 'Content-Type:application/json' ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed GET request"
    exit 2
fi

assert_json_value 'Btest value2' '.value' $DIR/curl.out

echo "OK"

runurl="${APIURL}/project/$test_proj/config/test.property3"

echo "TEST: PUT $runurl"

value="{\"key\":\"test.property3\", \"value\":\"Btest value3\"}"
# post
docurl -X PUT --data-binary "${value}"  -H 'Content-Type:application/json' ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed GET request"
    exit 2
fi

assert_json_value 'Btest value3' '.value' $DIR/curl.out

echo "OK"

runurl="${APIURL}/project/$test_proj/config"

echo "TEST: verify $runurl"
# get all config to verify
docurl -H Accept:application/json ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed POST request"
    exit 2
fi

API_XML_NO_WRAPPER=true $SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check result

assert_json_value "Btest value" ".\"test.property\"" $DIR/curl.out
assert_json_value "Btest value2" ".\"test.property2\"" $DIR/curl.out
assert_json_value "Btest value3" ".\"test.property3\"" $DIR/curl.out

echo "OK"

# now delete the test project
delete_project "$test_proj"
