#!/bin/bash

#test POST /api/11/projects
#using API v11, JSON format

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

echo "TEST: POST /api/11/projects"

test_proj="APICreateTest"

cat > $DIR/proj_create.post.json <<END
{
    "name": "$test_proj",
    "description":"test2",
    "config": {
        "test.property": "test value"
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
assert_json_value $test_proj ".name" $DIR/curl.out
assert_json_value "test value" .config['"test.property"'] $DIR/curl.out

echo "OK"

# now delete the test project

runurl="${APIURL}/project/$test_proj"
docurl -X DELETE  ${runurl} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed DELETE request"
    exit 2
fi



rm $DIR/proj_create.post.json
rm $DIR/curl.out

