#!/bin/bash

#test /api/jobs invalid project parameter

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

# now submit req
runurl="${APIURL}/jobs"

echo "TEST:  project parameter invalid for /api/jobs..."

params="project=DNEProject"

sh $DIR/api-expect-error.sh "${runurl}" "${params}" "project does not exist: DNEProject" || exit 2
echo "OK"

rm $DIR/curl.out

