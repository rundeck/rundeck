#!/bin/bash

#test  /api/job/{id}/executions
# set -x
DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

###
# setup: create a new job and acquire the ID
###

# job exec
args="echo testing /job/ID/executions result"

project="test"

#escape the string for xml
xmlargs=$($XMLSTARLET esc "$args")
xmlproj=$($XMLSTARLET esc "$project")

#produce job.xml content corresponding to the dispatch request
cat > $DIR/temp.out <<END
<joblist>
   <job>
      <name>test job</name>
      <group>test/api/executions</group>
      <description>Test the /job/ID/executions API endpoint</description>
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
jobid=$(uploadJob "$DIR/temp.out" "$project" 1)

###
# Test result of /job/ID/executions is 0 list
###

echo "TEST: job/${jobid}/executions should succeed with 0 results"

# now submit req
runurl="${APIURL}/job/${jobid}/executions"
params=""

# get listing
docurl  ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#verify 0 results

assert_json_value "0"  ".executions|length" $DIR/curl.out
echo "OK"

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

assert_json_not_null ".id" $DIR/curl.out
execid=$(jq -r ".id" < $DIR/curl.out)


if [  "" != "${execid}" ] ; then
    echo "OK"
else
    errorMsg "FAIL: expected run success message for execution id. (count: ${execcount}, id: ${execid})"
    exit 2
fi

###
# Test result of /job/ID/executions is 1 list
###

echo "TEST: job/id/executions should succeed with 1 results"

# now submit req
runurl="${APIURL}/job/${jobid}/executions"
params=""

# get listing
docurl ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#verify 1 results

assert_json_value "1" ".executions | length" $DIR/curl.out
assert_json_value "${execid}" ".executions[0].id" $DIR/curl.out
echo "OK"

###
# Test result of /job/ID/executions?status=succeeded is 1 list
###

echo "TEST: job/id/executions?status=succeeded should succeed with 1 results"

# now submit req
runurl="${APIURL}/job/${jobid}/executions"
params="status=succeeded"

api_waitfor_execution $execid || {
  errorMsg "Failed to wait for execution $execid to finish"
  exit 2
}

# get listing
docurl ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#verify 1 results

assert_json_value "1" ".executions | length" $DIR/curl.out
assert_json_value "${execid}" ".executions[0].id" $DIR/curl.out
echo "OK"


#####
# execute again
#####

origexecid=$execid

echo "TEST: job/id/run should succeed"

# now submit req
runurl="${APIURL}/job/${jobid}/run"
params=""
execargs="-opt2 a"

# get listing
$CURL -H "$AUTHHEADER" --data-urlencode "argString=${execargs}" ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#get execid

assert_json_not_null ".id" $DIR/curl.out
execid=$(jq -r ".id" < $DIR/curl.out)


if [  "" != "${execid}" ] ; then
    echo "OK"
else
    errorMsg "FAIL: expected run success message for execution id. (count: ${execcount}, id: ${execid})"
    exit 2
fi

#wait for execution to finish
api_waitfor_execution $execid || fail "Waiting for execution $execid to finish"
$SHELL $SRC_DIR/api-expect-exec-success.sh $execid || fail "Wrong exit status for exec $execid"

###
# Test result of /job/ID/executions paging params
###

echo "TEST: job/id/executions all results"

# now submit req
runurl="${APIURL}/job/${jobid}/executions"
params=""

# get listing
docurl  ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#verify 1 results

assert_json_value "2" ".executions | length" $DIR/curl.out
echo "OK"


###
# Test result of /job/ID/executions paging params
###

echo "TEST: job/id/executions max param"

# now submit req
runurl="${APIURL}/job/${jobid}/executions"
params="max=1"

# get listing
docurl  ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#verify 1 results

assert_json_value "1" ".executions | length" $DIR/curl.out
assert_json_value "${execid}" ".executions[0].id" $DIR/curl.out
echo "OK"


###
# Test result of /job/ID/executions paging params
###

echo "TEST: job/id/executions offset param"

# now submit req
runurl="${APIURL}/job/${jobid}/executions"
params="max=1&offset=1"

# get listing
docurl  ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#verify 1 results

assert_json_value "1" ".executions | length" $DIR/curl.out
assert_json_value "${origexecid}" ".executions[0].id" $DIR/curl.out
echo "OK"

#############



###
# Test result of /job/ID/executions arbitrary status param
###

echo "TEST: job/id/executions arbitrary status param"

# now submit req
runurl="${APIURL}/job/${jobid}/executions"
params="status=some_status"


docurl  ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2



assert_json_value "0" ".executions | length" $DIR/curl.out

echo "OK"

###
# Test result of /job/ID/executions bad id param
###

echo "TEST: job/id/executions invalid id param"

# now submit req
runurl="${APIURL}/job/fake/executions"
params=""

$SHELL $SRC_DIR/api-expect-error.sh ${runurl} "${params}" "Job ID does not exist: fake" 404 || exit 2
echo "OK"

####
# create job that will fail
####

# job exec
args="/bin/false this should fail"

project="test"

#escape the string for xml
xmlargs=$($XMLSTARLET esc "$args")
xmlproj=$($XMLSTARLET esc "$project")

#produce job.xml content corresponding to the dispatch request
cat > $DIR/temp.out <<END
<joblist>
   <job>
      <name>test job</name>
      <group>test/api/executions 2</group>
      <description>Test the /job/ID/executions API endpoint</description>
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
jobid=$(uploadJob "$DIR/temp.out" "$project"  1)


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

assert_json_not_null ".id" $DIR/curl.out
execid=$(jq -r ".id" < $DIR/curl.out)

if [ "" != "${execid}" ] ; then
    echo "OK"
else
    errorMsg "FAIL: expected run success message for execution id. (count: ${execcount}, id: ${execid})"
    exit 2
fi

# let job finish



api_waitfor_execution $execid || {
  errorMsg "Failed to wait for execution $execid to finish"
  exit 2
}

###
# Test result of /job/ID/executions?status=failed is 1 list
###

echo "TEST: job/id/executions?status=failed with 1 results"

# now submit req
runurl="${APIURL}/job/${jobid}/executions"
params="status=failed"

# get listing
docurl  ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#verify 1 results

assert_json_value "1" ".executions | length" $DIR/curl.out
assert_json_value "${execid}" ".executions[0].id" $DIR/curl.out
echo "OK"



####
# create job that will run for a while
####

# job exec
args="echo this job will be killed..."
args2="sleep 240"

project="test"

#escape the string for xml
xmlargs=$($XMLSTARLET esc "$args")
xmlargs2=$($XMLSTARLET esc "$args2")
xmlproj=$($XMLSTARLET esc "$project")

#produce job.xml content corresponding to the dispatch request
cat > $DIR/temp.out <<END
<joblist>
   <job>
      <name>test job</name>
      <group>test/api/executions 3</group>
      <description>Test the /job/ID/executions API endpoint</description>
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
        <command>
        <exec>$xmlargs2</exec>
        </command>
      </sequence>
   </job>
</joblist>

END

# now submit req
jobid=$(uploadJob "$DIR/temp.out" "$project" 1 "dupeOption=create")



###
# Run the chosen id, expect success message
###

# now submit req
runurl="${APIURL}/job/${jobid}/run"
params=""
execargs="-opt2 a"

# get listing
$CURL -H "$AUTHHEADER" --data-urlencode "argString=${execargs}" ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#get execid

assert_json_not_null ".id" $DIR/curl.out
execid=$(jq -r ".id" < $DIR/curl.out)

[ "" != "${execid}" ] || fail "expected run success message for execution id. (id: ${execid})"

###
# Test result of /job/ID/executions?status=running is 1 list
###

echo "TEST: job/id/executions?status=running with 1 results"

# now submit req
runurl="${APIURL}/job/${jobid}/executions"
params="status=running"

# get listing
docurl  ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#verify 1 results

assert_json_value "1" ".executions | length" $DIR/curl.out
assert_json_value "${execid}" ".executions[0].id" $DIR/curl.out
echo "OK"


# Abort the job

###
# Abort the running job
###

# now submit req
runurl="${APIURL}/execution/${execid}/abort"
params=""

echo "Aborting job: ${runurl}"

# get listing
docurl -X POST ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

# let pending abort finish

sleep 20


###
# Test result of /job/ID/executions?status=aborted is 1 list
###

echo "TEST: job/${jobid}/executions?status=aborted with 1 results"

# now submit req
runurl="${APIURL}/job/${jobid}/executions"
params="status=aborted"

# get listing
docurl  ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#verify 1 results

assert_json_value "1" ".executions | length" $DIR/curl.out
assert_json_value "${execid}" ".executions[0].id" $DIR/curl.out
echo "OK"
