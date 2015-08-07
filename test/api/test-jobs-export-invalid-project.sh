#!/bin/bash

#test output from /api/jobs/export with invalid project

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

# now submit req
runurl="${APIURL}/jobs/export"

echo "TEST: export RunDeck Jobs in jobs.xml format [invalid project parameter]"

params="project=DNEProject"

# expect error message
$SHELL $SRC_DIR/api-expect-error.sh "${runurl}" "${params}" "project does not exist: DNEProject" 404 || exit 2
echo "OK"

rm $DIR/curl.out
