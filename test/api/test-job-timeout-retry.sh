#!/bin/bash

#test  /api/job/{id}/run

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

###
# setup: create a new job and acquire the ID
###

# job exec
args="echo hello there ; sleep 30"

project=$2
if [ "" == "$2" ] ; then
    project="test"
fi

#escape the string for xml
xmlargs=$($XMLSTARLET esc "$args")
xmlproj=$($XMLSTARLET esc "$project")

#produce job.xml content corresponding to the dispatch request
cat > $DIR/temp.out <<END
<joblist>
   <job>
      <name>cli job</name>
      <group>api-test/job-run-timeout-retry</group>
      <description></description>
      <loglevel>INFO</loglevel>
      <timeout>3s</timeout>
      <retry>1</retry>
      <dispatch>
        <threadcount>1</threadcount>
        <keepgoing>true</keepgoing>
      </dispatch>
      <sequence>
        <command>
        <exec>$xmlargs</exec>
        </command>
      </sequence>
   </job>
</joblist>

END

jobid=$(uploadJob "$DIR/temp.out" "$project"  1 "")
if [ 0 != $? ] ; then
  errorMsg "failed job upload"
  exit 2
fi


###
# Run the chosen id, expect success message
###

echo "TEST: job/id/run should succeed"


# now submit req
runurl="${APIURL}/job/${jobid}/run"
params=""
execargs="-opt2 a"

# get listing
$CURL -H "$AUTHHEADER" --data-urlencode "argString=${execargs}" ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#get execid

execcount=$(xmlsel "//executions/@count" $DIR/curl.out)
execid=$(xmlsel "//executions/execution/@id" $DIR/curl.out)

if [ "1" == "${execcount}" -a "" != "${execid}" ] ; then
    :
else
    errorMsg "FAIL: expected run success message for execution id. (count: ${execcount}, id: ${execid})"
    exit 2
fi

#wait for execution to complete

api_waitfor_execution $execid false || fail "Failed waiting for execution $execid to complete"

# test execution status
#
runurl="${APIURL}/execution/${execid}"

params=""

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$(xmlsel "//executions/@count" $DIR/curl.out)
assert "1" "$itemcount" "execution count should be 1"
status=$(xmlsel "//executions/execution/@status" $DIR/curl.out)
assert "failed-with-retry" "$status" "execution status should be succeeded"
assert_xml_notblank "//executions/execution/retriedExecution/execution/@id" $DIR/curl.out
retryId=$(xmlsel "//executions/execution/retriedExecution/execution/@id" $DIR/curl.out)


#wait for retry 1 execution to complete

api_waitfor_execution $retryId false || fail "Failed waiting for execution $retryId to complete"


# test execution status
#
runurl="${APIURL}/execution/${retryId}"

params=""

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$(xmlsel "//executions/@count" $DIR/curl.out)
assert "1" "$itemcount" "execution count should be 1"
status=$(xmlsel "//executions/execution/@status" $DIR/curl.out)
assert "timedout" "$status" "execution status should be timedout"
assert_xml_value "" "//executions/execution/retriedExecution/execution/@id" $DIR/curl.out

echo "OK"


rm $DIR/curl.out

