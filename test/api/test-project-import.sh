#!/bin/bash

#test GET /api/14/project/name/import
#using API v14

# use api V14
API_VERSION=14

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh



##
# SETUP: create project, import job
##


assert_job_count(){
    projname=$1
    count=$2
    runurl="${APIURL}/project/$projname/jobs"
    docurl -D $DIR/headers.out ${runurl} > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed query request"
        exit 2
    fi
    assert_http_status 200 $DIR/headers.out
    assert_json_value $count 'length' $DIR/curl.out
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
    assert_json_value $count 'length' $DIR/curl.out
}

test_proj="APIImportTest"
test_proj2="APIImportTest2"

create_proj $test_proj
create_proj $test_proj2

runurl="${APIURL}/project/$test_proj/import"
params="jobUuidOption=preserve"

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

# test jobs  were imported

assert_job_count $test_proj '3'

# test  executions were imported

assert_execution_count $test_proj '6'

echo "OK"

##
# test 'preserve' uuidOption fails jobs that exist

runurl="${APIURL}/project/$test_proj2/import"

params="jobUuidOption=preserve"

echo "TEST: PUT $runurl?$params"

archivefile=$SRC_DIR/archive-test.zip

docurl -X PUT -H 'Content-Type: application/zip' -D $DIR/headers.out --data-binary @$archivefile \
    ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi
assert_http_status 200 $DIR/headers.out

assert_json_value 'failed' '.import_status' $DIR/curl.out

# test 0 jobs  were imported

assert_job_count $test_proj2 '0'

# test 3 adhoc executions were imported

assert_execution_count $test_proj2 '3'
echo "OK"


delete_proj $test_proj2
create_proj $test_proj2

runurl="${APIURL}/project/$test_proj2/import"
params="jobUuidOption=remove"

echo "TEST: PUT $runurl?$params"

archivefile=$SRC_DIR/archive-test.zip

docurl -X PUT -H 'Content-Type: application/zip' -D $DIR/headers.out --data-binary @$archivefile \
    ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi
assert_http_status 200 $DIR/headers.out

assert_json_value 'successful' '.import_status' $DIR/curl.out

# test jobs  were imported

assert_job_count $test_proj2 '3'

# test  executions were imported

assert_execution_count $test_proj2 '6'
echo "OK"

delete_proj $test_proj
delete_proj $test_proj2


#############################
## Test skip execution import
###############################
create_proj $test_proj

runurl="${APIURL}/project/$test_proj/import"
params="importExecutions=false"

echo "TEST: PUT $runurl?$params"

archivefile=$SRC_DIR/archive-test.zip

docurl -X PUT -H 'Content-Type: application/zip' -D $DIR/headers.out --data-binary @$archivefile \
    ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi
assert_http_status 200 $DIR/headers.out

assert_json_value 'successful' '.import_status' $DIR/curl.out

# test jobs  were imported

assert_job_count $test_proj '3'

# test  executions were imported

assert_execution_count $test_proj '0'
echo "OK"

delete_proj $test_proj




#rm $DIR/curl.out
