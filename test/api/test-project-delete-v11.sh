#!/bin/bash

#test DELETE /api/11/project/NAME
#using API v11, no xml result wrapper

# use api V11
API_VERSION=11
API_XML_NO_WRAPPER=true

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

test_proj="APIDeleteTest"

# now submit req
runurl="${APIURL}/projects"

echo "TEST: DELETE /api/11/project/$test_proj"

##
#SETUP: create project
##

cat > $DIR/proj_create.post <<END
<project>
    <name>$test_proj</name>
    <description>API test. Please delete me.</description>
    <config>
        <property key="test.property" value="test value"/>
    </config>
</project>
END

# get listing
docurl -X POST -D $DIR/headers.out --data-binary @$DIR/proj_create.post -H Content-Type:application/xml ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed POST request"
    exit 2
fi
assert_http_status 201 $DIR/headers.out

API_XML_NO_WRAPPER=true $SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check result
assert_xml_value "$test_proj" "/project/name" $DIR/curl.out


##
#TEST: delete project
##

runurl="${APIURL}/project/$test_proj"

# get listing
docurl -D $DIR/headers.out -X DELETE ${runurl} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed DELETE request"
    exit 2
fi
assert_http_status 204 $DIR/headers.out


echo "OK"


rm $DIR/proj_create.post
rm $DIR/curl.out
rm $DIR/headers.out

