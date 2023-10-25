#!/bin/bash

#test GET /api/11/project/name/import
#using API v11

# use api V11
API_VERSION=14

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh



##
# SETUP: create project, import job
##



create_proj(){
    projname=$1
    cat > $DIR/proj_create.post <<END
{
"name":"$projname",
"description":"test1",
"config": {
    "test.property":"test value",
    "project.execution.history.cleanup.enabled":"true",
    "project.execution.history.cleanup.retention.days":"1",
    "project.execution.history.cleanup.batch":"500",
    "project.execution.history.cleanup.retention.minimum":"0",
    "project.execution.history.cleanup.schedule":"0 0/1 * 1/1 * ? *"
    }
}
END

    runurl="${APIURL}/projects"

    # post
    docurl -X POST -D $DIR/headers.out --data-binary @$DIR/proj_create.post \
        -H Content-Type:application/json ${runurl}?${params} > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed POST request"
        exit 2
    fi
    rm $DIR/proj_create.post
    assert_http_status 201 $DIR/headers.out
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
    assert_json_value $count '.executions | length' $DIR/curl.out
}

test_proj="APIImportAndCleanHistoryTest"
#delete project if exists
set +e
#delete_proj $test_proj
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
assert_json_value 'successful' '.import_status' $DIR/curl.out

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
