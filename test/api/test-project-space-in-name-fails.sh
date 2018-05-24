#!/bin/bash

#test POST /api/${API_VERSION}/projects
#using API JSON format

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

JQ=`which jq`

if [ -z "$JQ" ] ; then
    errorMsg "FAIL: Can't test JSON format, install jq"
    exit 2
fi

# now submit req
runurl="${APIURL}/projects"

echo "TEST: POST ${runurl}"

test_proj="Invalid Project Name"

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
assert_http_status 400 $DIR/headers.out

rm $DIR/proj_create.post.json
rm $DIR/curl.out

