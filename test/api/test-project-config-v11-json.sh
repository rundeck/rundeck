#!/bin/bash

#test PUT /api/11/project/config
#using API v11, using json

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
runurl="${APIURL}/projects"
test_proj="APIConfigTest"

cat > $DIR/proj_create.post.json <<END
{
    "name": "$test_proj",
    "description":"test2",
    "config": {
        "test.property": "test value",
        "test.property2": "test value2"
    }
}
END

# get listing
docurl -X POST -D $DIR/headers.out --data-binary @$DIR/proj_create.post.json -H Content-Type:application/json ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed POST request"
    exit 2
fi
assert_http_status 201 $DIR/headers.out

#Check result
assert_json_value "$test_proj" .name  $DIR/curl.out
assert_json_value "test value" .config['"test.property"']  $DIR/curl.out
assert_json_value "test value2" .config['"test.property2"']  $DIR/curl.out
assert_json_null .config['"test.property3"']  $DIR/curl.out



runurl="${APIURL}/project/$test_proj/config"

echo "TEST: PUT $runurl"

cat > $DIR/proj_config.post.json <<END
{
        "test.property": "Btest value",
        "test.property3": "test value3",
}
END

# post
docurl -X PUT -D $DIR/headers.out --data-binary @$DIR/proj_config.post.json -H Content-Type:application/json ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed PUT request"
    exit 2
fi

assert_http_status 200 $DIR/headers.out


assert_json_value "Btest value" ".[\"test.property\"]"  $DIR/curl.out
assert_json_null ".[\"test.property2\"]"  $DIR/curl.out
assert_json_value "test value3" ".[\"test.property3\"]"  $DIR/curl.out


echo "OK"

# now delete the test project

runurl="${APIURL}/project/$test_proj"
docurl -X DELETE  ${runurl} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed DELETE request"
    exit 2
fi


rm $DIR/proj_create.post.json
rm $DIR/proj_config.post.json
rm $DIR/curl.out

