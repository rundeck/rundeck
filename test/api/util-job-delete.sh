#!/bin/bash


#Usage:
#   util-job-delete.sh <URL> <id>
#   Deletes specified job ID.
#

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

###
# DELETE the chosen id, expect success message
###

jobid=$2
shift

echo "Deleting job ID ${jobid}..."


# now submit req
runurl="${APIURL}/job/${jobid}"
params=""

# delete
$CURL -X DELETE ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

sh $DIR/api-test-success.sh $DIR/curl.out || exit 2

xmlsel "/result/success/message" -n $DIR/curl.out
    
rm $DIR/curl.out

