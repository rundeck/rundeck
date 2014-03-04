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
docurl -X POST --data-binary @$DIR/proj_create.post.json -H Content-Type:application/json ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed POST request"
    exit 2
fi

#Check result
name=$($JQ -r .name < $DIR/curl.out)
if [ "$test_proj" != "$name" ] ; then
    errorMsg "/project/name wrong value, expected $name"
    exit 2
fi
propval=$($JQ -r  .config['"test.property"'] < $DIR/curl.out)
if [ "test value" != "$propval" ] ; then
    errorMsg "/project/config/property[@key=test.property] wrong value, expected 'test value'"
    exit 2
fi

echo "OK"



rm $DIR/proj_create.post.json
rm $DIR/curl.out

