#!/bin/bash

#test output from /api/job/{id} when job ID does not exist


DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

###
# Export an invalid ID
###

echo "TEST: export job with wrong ID"


# now submit req
runurl="${APIURL}/job/9000"
params=""

# expect error message
$SHELL $SRC_DIR/api-expect-error.sh "${runurl}" "${params}" "Job ID does not exist: 9000" 404 || exit 2
echo "OK"


rm $DIR/curl.out

