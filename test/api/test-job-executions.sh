#!/bin/bash

#test  /api/job/{id}/executions

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

# now submit req
runurl="${APIURL}/project/$project/jobs/import"

params="dupeOption=create"

# specify the file for upload with curl, named "xmlBatch"
ulopts="-F xmlBatch=@$DIR/temp.out"

# get listing
docurl $ulopts  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#result will contain list of failed and succeeded jobs, in this
#case there should only be 1 failed or 1 succeeded since we submit only 1

succount=$($XMLSTARLET sel -T -t -v "/result/succeeded/@count" $DIR/curl.out)
jobid=$($XMLSTARLET sel -T -t -v "/result/succeeded/job/id" $DIR/curl.out)

if [ "1" != "$succount" -o "" == "$jobid" ] ; then
    errorMsg  "Upload was not successful."
    $XMLSTARLET sel -T -t -v "/result/failed" $DIR/curl.out
    exit 2
fi

###
# Test result of /job/ID/executions is 0 list
###

echo "TEST: job/id/executions should succeed with 0 results"

# now submit req
runurl="${APIURL}/job/${jobid}/executions"
params=""

# get listing
docurl  ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#verify 0 results

assert "0" $(xmlsel "/result/executions/@count" $DIR/curl.out) "Wrong number of executions"
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

execcount=$($XMLSTARLET sel -T -t -v "/result/executions/@count" $DIR/curl.out)
execid=$($XMLSTARLET sel -T -t -v "/result/executions/execution/@id" $DIR/curl.out)

if [ "1" == "${execcount}" -a "" != "${execid}" ] ; then
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

assert "1" $(xmlsel "/result/executions/@count" $DIR/curl.out) "Wrong number of executions"
assert "${execid}" $(xmlsel "/result/executions/execution/@id" $DIR/curl.out) "Wrong ID found"
echo "OK"

###
# Test result of /job/ID/executions?status=succeeded is 1 list
###

echo "TEST: job/id/executions?status=succeeded should succeed with 1 results"

# now submit req
runurl="${APIURL}/job/${jobid}/executions"
params="status=succeeded"

sleep 6

# get listing
docurl ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#verify 1 results

assert "1" $(xmlsel "/result/executions/@count" $DIR/curl.out) "Wrong number of executions"
assert "${execid}" $(xmlsel "/result/executions/execution/@id" $DIR/curl.out) "Wrong ID found"
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

execcount=$($XMLSTARLET sel -T -t -v "/result/executions/@count" $DIR/curl.out)
execid=$($XMLSTARLET sel -T -t -v "/result/executions/execution/@id" $DIR/curl.out)

if [ "1" == "${execcount}" -a "" != "${execid}" ] ; then
    echo "OK"
else
    errorMsg "FAIL: expected run success message for execution id. (count: ${execcount}, id: ${execid})"
    exit 2
fi

#wait for execution to finish
rd-queue follow -q -e $execid || fail "Waiting for execution $execid to finish"
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

assert "2" $(xmlsel "/result/executions/@count" $DIR/curl.out) "Wrong number of executions"
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

assert "1" $(xmlsel "/result/executions/@count" $DIR/curl.out) "Wrong number of executions"
assert "$execid" $(xmlsel "/result/executions/execution/@id" $DIR/curl.out) "Wrong exec id"
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

assert "1" $(xmlsel "/result/executions/@count" $DIR/curl.out) "Wrong number of executions"
assert "$origexecid" $(xmlsel "/result/executions/execution/@id" $DIR/curl.out) "Wrong exec id"
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



assert "0" $(xmlsel "/result/executions/@count" $DIR/curl.out) "Wrong number of executions"

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
runurl="${APIURL}/project/$project/jobs/import"

params="dupeOption=create"

# specify the file for upload with curl, named "xmlBatch"
ulopts="-F xmlBatch=@$DIR/temp.out"

# get listing
docurl $ulopts  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#result will contain list of failed and succeeded jobs, in this
#case there should only be 1 failed or 1 succeeded since we submit only 1

succount=$($XMLSTARLET sel -T -t -v "/result/succeeded/@count" $DIR/curl.out)
jobid=$($XMLSTARLET sel -T -t -v "/result/succeeded/job/id" $DIR/curl.out)

if [ "1" != "$succount" -o "" == "$jobid" ] ; then
    errorMsg  "Upload was not successful."
    $XMLSTARLET sel -T -t -v "/result/failed" $DIR/curl.out
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

execcount=$($XMLSTARLET sel -T -t -v "/result/executions/@count" $DIR/curl.out)
execid=$($XMLSTARLET sel -T -t -v "/result/executions/execution/@id" $DIR/curl.out)

if [ "1" == "${execcount}" -a "" != "${execid}" ] ; then
    echo "OK"
else
    errorMsg "FAIL: expected run success message for execution id. (count: ${execcount}, id: ${execid})"
    exit 2
fi

# let job finish
sleep 6

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

assert "1" $(xmlsel "/result/executions/@count" $DIR/curl.out) "Wrong number of executions"
assert "${execid}" $(xmlsel "/result/executions/execution/@id" $DIR/curl.out) "Wrong ID found"
echo "OK"



####
# create job that will run for a while
####

# job exec
args="echo this job will be killed..."
args2="sleep 120"

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
runurl="${APIURL}/project/$project/jobs/import"

params="dupeOption=create"

# specify the file for upload with curl, named "xmlBatch"
ulopts="-F xmlBatch=@$DIR/temp.out"

# get listing
docurl $ulopts  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#result will contain list of failed and succeeded jobs, in this
#case there should only be 1 failed or 1 succeeded since we submit only 1

succount=$($XMLSTARLET sel -T -t -v "/result/succeeded/@count" $DIR/curl.out)
jobid=$($XMLSTARLET sel -T -t -v "/result/succeeded/job/id" $DIR/curl.out)

if [ "1" != "$succount" -o "" == "$jobid" ] ; then
    errorMsg  "Upload was not successful."
    $XMLSTARLET sel -T -t -v "/result/failed" $DIR/curl.out
    exit 2
fi


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

execcount=$($XMLSTARLET sel -T -t -v "/result/executions/@count" $DIR/curl.out)
execid=$($XMLSTARLET sel -T -t -v "/result/executions/execution/@id" $DIR/curl.out)

[ "1" == "${execcount}" -a "" != "${execid}" ] || fail "expected run success message for execution id. (count: ${execcount}, id: ${execid})"

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

assert "1" $(xmlsel "/result/executions/@count" $DIR/curl.out) "Wrong number of executions"
assert "${execid}" $(xmlsel "/result/executions/execution/@id" $DIR/curl.out) "Wrong ID found"
echo "OK"


# Abort the job

###
# Abort the running job
###

# now submit req
runurl="${APIURL}/execution/${execid}/abort"
params=""

# get listing
docurl -X POST ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

# let pending abort finish

sleep 3


###
# Test result of /job/ID/executions?status=aborted is 1 list
###

echo "TEST: job/id/executions?status=aborted with 1 results"

# now submit req
runurl="${APIURL}/job/${jobid}/executions"
params="status=aborted"

# get listing
docurl  ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#verify 1 results

assert "1" $(xmlsel "/result/executions/@count" $DIR/curl.out) "Wrong number of executions"
assert "${execid}" $(xmlsel "/result/executions/execution/@id" $DIR/curl.out) "Wrong ID found"
echo "OK"
