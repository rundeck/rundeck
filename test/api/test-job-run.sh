#!/bin/bash

#test  /api/job/{id}/run

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

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

#produce job.xml content corresponding to the dispatch request
cat > $DIR/temp.out <<END
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
   <job>
      <name>cli job2</name>
      <group>api-test/job-run</group>
      <description></description>
      <loglevel>INFO</loglevel>
      <dispatch>
        <threadcount>1</threadcount>
        <keepgoing>true</keepgoing>
      </dispatch>
      <nodefilters>
        <filter>.*</filter>
      </nodefilters>
      <nodesSelectedByDefault>false</nodesSelectedByDefault>
      <sequence>
        <command>
        <exec>$xmlargs</exec>
        </command>
      </sequence>
   </job>
</joblist>

END

jobid=$(uploadJob "$DIR/temp.out" "$project"  2 "" )
if [ 0 != $? ] ; then
  errorMsg "failed job upload"
  exit 2
fi
jobid2=$(jq -r ".succeeded[1].id" $DIR/curl.out)


###
# Run the chosen id, expect success message
###

echo "TEST: POST job/id/run should succeed"


execid=$(runjob "$jobid" "-opt2 a")

#wait for execution to complete

api_waitfor_execution $execid || fail "Failed waiting for execution $execid to complete"

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

assert_json_value "succeeded" ".status" $DIR/curl.out

echo "OK"

###
# should fail if dont select any node and nodesSelectedByDefault is false
###

echo "TEST: POST job/id/run should fail"


execid=$(runjob "$jobid2")

#wait for execution to complete

api_waitfor_execution $execid || fail "Failed waiting for execution $execid to complete"

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

assert_json_value "failed" ".status" $DIR/curl.out

echo "OK"

###
# should succeeded if select any node and nodesSelectedByDefault is false
###

echo "TEST: POST job/id/run should fail"


execid=$(runjob "$jobid2" '' '{ "filter":"name: .*" }')

#wait for execution to complete

api_waitfor_execution $execid || fail "Failed waiting for execution $execid to complete"

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

assert_json_value "succeeded" ".status" $DIR/curl.out

echo "OK"


###
# Specify option parameters in json
###

echo "TEST: POST job/id/run with JSON"

execid=$(runjob "$jobid" '' '{"options":{"opt1":"xyz","opt2":"def"}}')

#wait for execution to complete

api_waitfor_execution $execid || fail "Failed waiting for execution $execid to complete"

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
assert_json_value "succeeded" ".status" $DIR/curl.out

assert_json_value "-opt1 xyz -opt2 def" ".argstring" $DIR/curl.out
assert_json_value "xyz" ".job.options.opt1" $DIR/curl.out
assert_json_value "def" ".job.options.opt2" $DIR/curl.out

echo "OK"



###
# Specify option parameters individually
###

echo "TEST: POST job/id/run with option parameters"


# now submit req
runurl="${APIURL}/job/${jobid}/run"
params=""

# get listing
$CURL -H "$AUTHHEADER" -X POST --data-urlencode "option.opt2=a" --data-urlencode "option.opt1=xyz" \
  ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#get execid

assert_json_not_null ".id" $DIR/curl.out
execid=$(jq -r ".id" < $DIR/curl.out)

if [ "" != "${execid}" ] ; then
    :
else
    errorMsg "FAIL: expected run success message for execution id. (count: ${execcount}, id: ${execid})"
    exit 2
fi

#wait for execution to complete

api_waitfor_execution $execid || fail "Failed waiting for execution $execid to complete"

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

assert_json_value "succeeded" ".status" $DIR/curl.out

assert_json_value "-opt1 xyz -opt2 a" ".argstring" $DIR/curl.out
assert_json_value "xyz" ".job.options.opt1" $DIR/curl.out
assert_json_value "a" ".job.options.opt2" $DIR/curl.out

echo "OK"

echo "TEST: GET job/id/run should fail 405"


# now submit req
runurl="${APIURL}/job/${jobid}/run"
params=""
execargs="-opt2 a"

# let job finish executing
sleep 2

# get listing
$CURL -H "$AUTHHEADER" -D $DIR/headers.out -G --data-urlencode "argString=${execargs}" ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

ecode=405

#expect header code
grep "HTTP/1.1 ${ecode}" -q $DIR/headers.out
if [ 0 != $? ] ; then
    errorMsg "FAIL: expected ${ecode} message, but was:"
    grep 'HTTP/1.1' $DIR/headers.out
    exit 2
fi

echo "OK"
###
# Run the chosen id, leave off required option value
###

echo "TEST: POST job/id/run without required opt should fail"


# now submit req
runurl="${APIURL}/job/${jobid}/run"
params=""
execargs=""

# let job finish executing
sleep 2

# get listing
$CURL -H "$AUTHHEADER" -X POST --data-urlencode "argString=${execargs}" ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-error.sh $DIR/curl.out "Job options were not valid: Option 'opt2' is required." || exit 2

echo "OK"

rm $DIR/curl.out

