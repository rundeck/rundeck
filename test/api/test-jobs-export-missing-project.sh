#!/bin/bash

#test output from /api/jobs/export with missing project parameter

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

# now submit req
runurl="${APIURL}/jobs/export"

echo "TEST: export RunDeck Jobs in jobs.xml format [missing project parameter]"

params="project="

# expect error message
$SHELL $SRC_DIR/api-expect-error.sh "${runurl}" "${params}" "parameter \"project\" is required" || exit 2
echo "OK"

rm $DIR/curl.out

