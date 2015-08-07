#!/bin/bash

#test PUT /api/11/project/config
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


runurl="${APIURL}/project/$test_proj/config"

echo "TEST: PUT $runurl"

cat > $DIR/proj_config.post <<END
<config>
    <property key="test.property" value="Btest value"/>
    <property key="test.property3" value="test value3"/>
</config>
END

# post
docurl -X PUT -D $DIR/headers.out --data-binary @$DIR/proj_config.post -H Content-Type:application/xml ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed POST request"
    exit 2
fi
assert_http_status 200 $DIR/headers.out

API_XML_NO_WRAPPER=true $SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

assert_xml_value "Btest value" "/config/property[@key='test.property']/@value" $DIR/curl.out
assert_xml_value "" "/config/property[@key='test.property2']/@value" $DIR/curl.out
assert_xml_value "test value3" "/config/property[@key='test.property3']/@value" $DIR/curl.out



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

