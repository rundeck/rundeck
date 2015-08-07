#!/bin/bash

#Usage: 
#    util-job-executions.sh <URL> <jobid> [status]

DIR=$(cd `dirname $0` && pwd)

source $DIR/include.sh

jobid=$1
shift

state=$1
shift

# now submit req
runurl="${APIURL}/job/${jobid}/executions"

echo "# Listing Executions for job ${jobid}..."

params="status=${state}"

echo "url: ${runurl}?${params}"

# get listing
docurl  ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$(xmlsel "/result/executions/@count" $DIR/curl.out)
echo "$itemcount Executions"    
if [ "0" != "$itemcount" ] ; then
    #echo all on one line
    $XMLSTARLET sel -T -t -m "/result/executions/execution" -o "[" -v "@id" -o "](" -v "@status"  -o ") " -v "description" -o ": " -v "job/name" -n $DIR/curl.out
fi

#rm $DIR/curl.out

