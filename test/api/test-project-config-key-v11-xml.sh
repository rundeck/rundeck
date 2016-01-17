#!/bin/bash

#test GET /api/11/project/name/config/key
#using API v11, no xml result wrapper

# use api V11
API_VERSION=11
API_XML_NO_WRAPPER=true

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

# now submit req
runurl="${APIURL}/projects"
test_proj="APIConfigTest"


##
# setup: create project
##
cat > $DIR/proj_create.post <<END
<project>
    <name>$test_proj</name>
    <description>test1</description>
    <config>
        <property key="test.property" value="test value"/>
        <property key="test.property2" value="test value2"/>
    </config>
</project>
END

# post
docurl -X POST -D $DIR/headers.out --data-binary @$DIR/proj_create.post -H Content-Type:application/xml ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed POST request"
    exit 2
fi
assert_http_status 201 $DIR/headers.out

API_XML_NO_WRAPPER=true $SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check result

assert_xml_value $test_proj /project/name $DIR/curl.out
assert_xml_value "test value" "/project/config/property[@key='test.property']/@value" $DIR/curl.out
assert_xml_value "test value2" "/project/config/property[@key='test.property2']/@value" $DIR/curl.out
assert_xml_value "" "/project/config/property[@key='test.property3']/@value" $DIR/curl.out


runurl="${APIURL}/project/$test_proj/config/test.property"

echo "TEST: GET $runurl"

# post
docurl -H 'Accept:application/xml' ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed GET request"
    exit 2
fi

assert_xml_value 'test value' '/property/@value' $DIR/curl.out

echo "OK"

runurl="${APIURL}/project/$test_proj/config/test.property2"

echo "TEST: GET $runurl"

# post
docurl -H 'Accept:application/xml' ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed GET request"
    exit 2
fi

assert_xml_value 'test value2' '/property/@value' $DIR/curl.out

echo "OK"

runurl="${APIURL}/project/$test_proj/config/test.property"

echo "TEST: PUT $runurl"

value="<property key=\"test.property\" value=\"Btest value\"/>"
# post
docurl -X PUT --data-binary "${value}" -H 'Content-Type:application/xml' ${runurl} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed GET request"
    exit 2
fi

assert_xml_value 'Btest value' '/property/@value' $DIR/curl.out

echo "OK"

runurl="${APIURL}/project/$test_proj/config/test.property2"

echo "TEST: PUT $runurl"

value="<property key=\"test.property2\" value=\"Btest value2\"/>"
# post
docurl -X PUT --data-binary "${value}" -H 'Content-Type:application/xml' ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed GET request"
    exit 2
fi

assert_xml_value 'Btest value2' '/property/@value' $DIR/curl.out

echo "OK"

runurl="${APIURL}/project/$test_proj/config/test.property3"

echo "TEST: PUT $runurl"

value="<property key=\"test.property3\" value=\"Btest value3\"/>"
# post
docurl -X PUT --data-binary "${value}"  -H 'Content-Type:application/xml' ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed GET request"
    exit 2
fi

assert_xml_value 'Btest value3' '/property/@value' $DIR/curl.out

echo "OK"

runurl="${APIURL}/project/$test_proj/config"

echo "TEST: verify $runurl"
# get all config to verify
docurl -H Accept:application/xml ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed POST request"
    exit 2
fi

API_XML_NO_WRAPPER=true $SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check result

assert_xml_value "Btest value" "/config/property[@key='test.property']/@value" $DIR/curl.out
assert_xml_value "Btest value2" "/config/property[@key='test.property2']/@value" $DIR/curl.out
assert_xml_value "Btest value3" "/config/property[@key='test.property3']/@value" $DIR/curl.out

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

