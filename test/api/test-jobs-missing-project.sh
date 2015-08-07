#!/bin/bash

#test /api/jobs requires project parameter

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

# now submit req
runurl="${APIURL}/jobs"

echo "TEST: require project parameter for /api/jobs..."

params="project="

$SHELL $SRC_DIR/api-expect-error.sh "${runurl}" "${params}" "parameter \"project\" is required" || exit 2
echo "OK"


rm $DIR/curl.out

