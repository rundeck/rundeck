#!/bin/bash

#Usage: 
#    util-job-run.sh <URL> ID [args...]

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

jobid=$1
shift

execargs="$*"
# now submit req
runurl="${APIURL}/job/${jobid}/run"

echo "# Run JOB ${jobid} -- ${execargs}"

params=""

# get listing
$CURL --data-urlencode "argString=${execargs}" ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

sh $DIR/api-test-success.sh $DIR/curl.out || exit 2

# job list query doesn't wrap result in common result wrapper
#If <result error="true"> then an error occured.
xmlsel "/result/success/message" -n $DIR/curl.out
$XMLSTARLET sel -T -t -o "Execution started with ID: " -v "/result/execution/@id" -n  $DIR/curl.out

rm $DIR/curl.out

