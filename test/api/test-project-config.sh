#!/bin/bash

#test PUT /api/14/project/config
#using API v14

# use api V14
API_VERSION=14

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

# now submit req
runurl="${APIURL}/projects"
test_proj="APIConfigTest"


##
# setup: create project
##
cat > $DIR/proj_create.post <<END
{
    "name":"$test_proj",
    "description":"test1",
    "config":{
        "test.property":"test value",
        "test.property2":"test value2"
    }
}
END

# post
docurl -X POST -D $DIR/headers.out --data-binary @$DIR/proj_create.post -H Content-Type:application/json ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed POST request"
    exit 2
fi
assert_http_status 201 $DIR/headers.out

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check result

assert_json_value $test_proj '.name' $DIR/curl.out
assert_json_value "test value" ".config.\"test.property\"" $DIR/curl.out
assert_json_value "test value2" ".config.\"test.property2\"" $DIR/curl.out
assert_json_null ".config.\"test.property3\"" $DIR/curl.out


runurl="${APIURL}/project/$test_proj/config"

echo "TEST: PUT $runurl"

cat > $DIR/proj_config.post <<END
{
"test.property":"Btest value",
"test.property3":"test value3"
}
END

# post
docurl -X PUT -D $DIR/headers.out --data-binary @$DIR/proj_config.post -H Content-Type:application/json ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed POST request"
    exit 2
fi
assert_http_status 200 $DIR/headers.out

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

assert_json_value "Btest value" ".config.\"test.property\"" $DIR/curl.out
assert_json_null ".config.\"test.property2\"" $DIR/curl.out
assert_json_value "test value3" ".config.\"test.property3\"" $DIR/curl.out



echo "OK"

# now delete the test project

runurl="${APIURL}/project/$test_proj"
docurl -X DELETE  ${runurl} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed DELETE request"
    exit 2
fi


rm $DIR/proj_create.post
rm $DIR/proj_config.post
rm $DIR/curl.out

