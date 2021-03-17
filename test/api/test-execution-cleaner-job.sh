#!/bin/bash

#test GET /api/11/project/name/import
#using API v11

# use api V11
API_VERSION=14
API_XML_NO_WRAPPER=true

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh



##
# SETUP: create project, import job
##



create_proj(){
    projname=$1
    cat > $DIR/proj_create.post <<END
<project>
    <name>$projname</name>
    <description>test1</description>
    <config>
        <property key="test.property" value="test value"/>
        <property key="project.execution.history.cleanup.enabled" value="true"/>
        <property key="project.execution.history.cleanup.retention.days" value="1"/>
        <property key="project.execution.history.cleanup.batch" value="500"/>
        <property key="project.execution.history.cleanup.retention.minimum" value="0"/>
        <property key="project.execution.history.cleanup.schedule" value="0 0/1 * 1/1 * ? *"/>
    </config>
</project>
END

    runurl="${APIURL}/projects"

    # post
    docurl -X POST -D $DIR/headers.out --data-binary @$DIR/proj_create.post \
        -H Content-Type:application/xml ${runurl}?${params} > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed POST request"
        exit 2
    fi
    rm $DIR/proj_create.post
    assert_http_status 201 $DIR/headers.out
}
delete_proj(){
    projname=$1

    runurl="${APIURL}/project/$projname"
    docurl -X DELETE  ${runurl} > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed DELETE request"
        exit 2
    fi
}
assert_execution_count(){
    projname=$1
    count=$2
    runurl="${APIURL}/project/${projname}/executions"
    docurl -D $DIR/headers.out ${runurl} > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed query request"
        exit 2
    fi
    assert_http_status 200 $DIR/headers.out
    assert_xml_value $count '/executions/@count' $DIR/curl.out
}

test_proj="APIImportAndCleanHistoryTest"
#delete project if exists
set +e
delete_proj $test_proj
set -e
create_proj $test_proj

runurl="${APIURL}/project/$test_proj/import"
params="jobUuidOption=remove"

echo "TEST: PUT $runurl"

archivefile=$SRC_DIR/archive-test.zip

docurl -X PUT -H 'Content-Type: application/zip' -D $DIR/headers.out --data-binary @$archivefile \
    ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi
assert_http_status 200 $DIR/headers.out

assert_xml_value 'successful' '/import/@status' $DIR/curl.out

# test  executions were imported

assert_execution_count $test_proj '6'

date

echo waiting cleaner job
sleep 120
echo end waiting cleaner job
date

assert_execution_count $test_proj '0'

echo "OK"

delete_proj $test_proj

#rm $DIR/curl.out
