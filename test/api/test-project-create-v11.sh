#!/bin/bash

#test POST /api/11/projects
#using API v11, no xml result wrapper

# use api V11
API_VERSION=11
API_XML_NO_WRAPPER=true

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

# now submit req
runurl="${APIURL}/projects"

echo "TEST: POST /api/11/projects"

test_proj="APICreateTest"

cat > $DIR/proj_create.post <<END
<project>
    <name>$test_proj</name>
    <description>test1</description>
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
egrep -q 'HTTP/1.1 201' $DIR/headers.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: Expected 201 result"
    egrep 'HTTP/1.1' $DIR/headers.out
    exit 2
fi

API_XML_NO_WRAPPER=true sh $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check result
name=$($XMLSTARLET sel -T -t -v "/project/name" $DIR/curl.out)
if [ "$test_proj" != "$name" ] ; then
    errorMsg "/project/name wrong value, expected $name"
    exit 2
fi
propval=$($XMLSTARLET sel -T -t -v "/project/config/property[@key=test.property]/@value" $DIR/curl.out)
if [ "test value" != "$propval" ] ; then
    errorMsg "/project/config/property[@key=test.property] wrong value, expected 'test value'"
    exit 2
fi

echo "OK"

# now delete the test project

runurl="${APIURL}/project/$test_proj"
docurl -X DELETE  ${runurl} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed DELETE request"
    exit 2
fi


rm $DIR/proj_create.post
rm $DIR/curl.out

