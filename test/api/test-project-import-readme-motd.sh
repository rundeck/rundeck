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


assert_readme(){
    projname=$1
    runurl="${APIURL}/project/$projname/readme.md"
    docurl -D $DIR/headers.out ${runurl} > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed query request"
        exit 2
    fi
    assert_http_status 200 $DIR/headers.out
    value=$(cat $DIR/curl.out)


    assert "this is a readme file" "$value"
}

assert_motd(){
    projname=$1
    runurl="${APIURL}/project/$projname/motd.md"
    docurl -D $DIR/headers.out ${runurl} > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed query request"
        exit 2
    fi
    assert_http_status 200 $DIR/headers.out
    value=$(cat "$DIR/curl.out")
    assert "this is a message of the day" "$value"
}

test_proj="APIImportTestReame"
create_proj $test_proj

runurl="${APIURL}/project/$test_proj/import"
params="jobUuidOption=remove&&importConfig=true"

echo "TEST: PUT $runurl"

archivefile=$SRC_DIR/archive-test-readme.zip

docurl -X PUT -H 'Content-Type: application/zip' -D $DIR/headers.out --data-binary @$archivefile \
    ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi
assert_http_status 200 $DIR/headers.out

assert_json_value 'successful' '.import_status' $DIR/curl.out

# testing readme and motd
assert_readme $test_proj
assert_motd $test_proj

echo "OK"

delete_proj $test_proj

