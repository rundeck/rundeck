#!/bin/bash

#test  /api/job/{id}/run with runAtTime
set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
source "${DIR}/include.sh"

###
# setup: create a new job and acquire the ID
###

# job exec
args="echo hello there"

project=$2
if [ "" == "$2" ] ; then
    project="test"
fi

#escape the string for xml
xmlargs=$($XMLSTARLET esc "$args")
xmlproj=$($XMLSTARLET esc "$project")
datewhen(){
    local secs=$1
  #determine h:m:s to run, 10 seconds from now
    local NDATES=$(date '+%s')
    NDATES=$(( $NDATES + $secs ))
    local osname=$(uname)
    if [ "Darwin" = "$osname" ] ; then
        env TZ=GMT date -r "$NDATES" '+%S %M %H %d %m %a %Y %z'
    else
        env TZ=GMT date --date="@$NDATES" '+%S %M %H %d %m %a %Y %z'
    fi
}

schedule_datetime=$(datewhen 50)
# Convert to uppercase (for weekday)
tokens=($schedule_datetime)
seconds=${tokens[0]}
minutes=${tokens[1]}
hours=${tokens[2]}
day=${tokens[3]}
month=${tokens[4]}
weekday=${tokens[5]}
year=${tokens[6]}
timezone=${tokens[7]}
#unixts=$(env TZ=GMT date -d "${year}/${month}/${day} ${hours}:${minutes}:${seconds}" +"%s")
runtime="${year}-${month}-${day}T${hours}:${minutes}:${seconds}.000+0000"
echo "Schedule time is: $runtime (unix ts: $unixts)"

#produce job.xml content corresponding to the dispatch request
cat > "${DIR}/temp.out" <<END
<joblist>
   <job>
      <name>cli job</name>
      <group>api-test/job-run</group>
      <description></description>
      <loglevel>INFO</loglevel>
      <context>
          <project>$xmlproj</project>
          <options>
              <option name="opt1" value="testvalue" required="true"/>
              <option name="opt2" values="a,b,c" required="true"/>
          </options>
      </context>
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

# now submit req
runurl="${APIURL}/project/${project}/jobs/import"

params=""

# specify the file for upload with curl, named "xmlBatch"
ulopts="-F xmlBatch=@${DIR}/temp.out"

# get listing
docurl $ulopts "${runurl}?${params}" > "$DIR/curl.out"
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request: ${runurl}?${params}"
    exit 2
fi

$SHELL "${SRC_DIR}/api-test-success.sh" "${DIR}/curl.out" || exit 2

#result will contain list of failed and succeeded jobs, in this
#case there should only be 1 failed or 1 succeeded since we submit only 1

succount=$($XMLSTARLET sel -T -t -v "/result/succeeded/@count" "$DIR/curl.out")
jobid=$($XMLSTARLET sel -T -t -v "/result/succeeded/job/id" "$DIR/curl.out")

if [ "1" != "$succount" ] || [ "" == "$jobid" ] ; then
    errorMsg  "Upload was not successful."
    exit 2
fi


###
# Run the chosen id, expect success message
###

echo "TEST: POST job/id/run should succeed with future time"


# now submit req
runurl="${APIURL}/job/${jobid}/run"
params=""
execargs="-opt2 a"

# get listing
$CURL -H "$AUTHHEADER" -X POST --data-urlencode "argString=${execargs}" --data-urlencode "runAtTime=${runtime}" "${runurl}?${params}" > "$DIR/curl.out" || fail "failed request: ${runurl}"

$SHELL "$SRC_DIR/api-test-success.sh" "$DIR/curl.out" || exit 2

#get execid

execcount=$($XMLSTARLET sel -T -t -v "/result/executions/@count" "$DIR/curl.out")
execid=$($XMLSTARLET sel -T -t -v "/result/executions/execution/@id" "$DIR/curl.out")
actualDateTime=$($XMLSTARLET sel -T -t -v "/result/executions/execution/date-started" "$DIR/curl.out")

# <date-started unixtime='1470324418000'>2016-08-04T15:26:58Z</date-started>
expectedDateTime="${year}-${month}-${day}T${hours}:${minutes}:${seconds}Z"

if [ "${expectedDateTime}" != "${actualDateTime}" ] ; then
    errorMsg "FAIL: date started is ${actualDateTime}, expected: ${expectedDateTime} (time: ${runtime})"
    exit 2
fi

if [ "1" == "${execcount}" ] && [ "" != "${execid}" ] ; then
    :
else
    errorMsg "FAIL: expected run success message for execution id. (count: ${execcount}, id: ${execid})"
    exit 2
fi

#wait for execution to complete

api_waitfor_execution "$execid" true

if [ 0 != $? ]; then
    fail "Failed waiting for execution $execid to complete"
fi

# test execution status
#
runurl="${APIURL}/execution/${execid}"

params=""

# get listing
docurl "${runurl}?${params}" > "$DIR/curl.out"
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL "$SRC_DIR/api-test-success.sh" "$DIR/curl.out"
if [ 0 != $? ] ; then
    errorMsg "FAIL: api-test-success failed"
    cat "$DIR/curl.out"
    exit 2
fi

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/executions/@count" "$DIR/curl.out")
assert "1" "$itemcount" "execution count should be 1"
status=$($XMLSTARLET sel -T -t -v "/result/executions/execution/@status" "$DIR/curl.out")
assert "succeeded" "$status" "execution status should be succeeded"

echo "OK"

echo "TEST: GET job/id/run should fail 405"


# now submit req
runurl="${APIURL}/job/${jobid}/run"
params=""
execargs="-opt2 a"

# let job finish executing
sleep 2

# get listing
$CURL -H "$AUTHHEADER" -D "$DIR/headers.out" -G --data-urlencode "argString=${execargs}" --data-urlencode "runAtTime=${runtime}" "${runurl}?${params}" > "$DIR/curl.out" || fail "failed request: ${runurl}"

ecode=405

#expect header code
grep "HTTP/1.1 ${ecode}" -q "$DIR/headers.out"
if [ 0 != $? ] ; then
    errorMsg "FAIL: expected ${ecode} message, but was:"
    grep 'HTTP/1.1' "$DIR/headers.out"
    exit 2
fi

echo "OK"
###
# Run the chosen id, pass a runAtTime in the past
###

echo "TEST: POST job/id/run with scheduled time in the past should fail"


# now submit req
runurl="${APIURL}/job/${jobid}/run"
params=""
execargs="-opt2 b"

# let job finish executing
sleep 2

# get listing
$CURL -H "$AUTHHEADER" -X POST --data-urlencode "argString=${execargs}" --data-urlencode "runAtTime=${runtime}" "${runurl}?${params}" > "$DIR/curl.out" || fail "failed request: ${runurl}"

$SHELL "$SRC_DIR/api-test-error.sh" "$DIR/curl.out" "Execution failed: A job cannot be scheduled for a time in the past" || exit 2

echo "OK"

###
# Run the chosen id, pass an invalid runAtTime
###

echo "TEST: POST job/id/run with invalid schedule time"


# now submit req
runurl="${APIURL}/job/${jobid}/run"
params=""
execargs="-opt2 b"

# let job finish executing
sleep 5

# get listing
$CURL -H "$AUTHHEADER" -X POST --data-urlencode "argString=${execargs}" --data-urlencode "runAtTime=1999/01/01 11:10:01.000+0000" "${runurl}?${params}" > "$DIR/curl.out" || fail "failed request: ${runurl}"

$SHELL "$SRC_DIR/api-test-error.sh" "$DIR/curl.out" "Execution failed: Invalid date/time format, only ISO 8601 is supported" || exit 2

echo "OK"


rm "$DIR/curl.out"
