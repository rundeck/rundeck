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
      <name>cli job</name>
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

# now submit req
runurl="${APIURL}/project/$project/jobs/import"

params=""

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
jobid=$($XMLSTARLET sel -T -t -v "/result/succeeded/job[@index=1]/id" $DIR/curl.out)
jobid2=$($XMLSTARLET sel -T -t -v "/result/succeeded/job[@index=2]/id" $DIR/curl.out)

if [ "2" != "$succount" -o "" == "$jobid" ] ; then
    errorMsg  "Upload was not successful."
    exit 
fi


###
# Run the chosen id, expect success message
###

echo "TEST: POST job/id/run should succeed"


# now submit req
runurl="${APIURL}/job/${jobid}/run"
params=""
execargs="-opt2 a"

# get listing
$CURL -H "$AUTHHEADER" -X POST --data-urlencode "argString=${execargs}" ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#get execid

execcount=$($XMLSTARLET sel -T -t -v "/result/executions/@count" $DIR/curl.out)
execid=$($XMLSTARLET sel -T -t -v "/result/executions/execution/@id" $DIR/curl.out)

if [ "1" == "${execcount}" -a "" != "${execid}" ] ; then
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

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/executions/@count" $DIR/curl.out)
assert "1" "$itemcount" "execution count should be 1"
status=$($XMLSTARLET sel -T -t -v "/result/executions/execution/@status" $DIR/curl.out)
assert "succeeded" "$status" "execution status should be succeeded"

echo "OK"

###
# should fail if dont select any node and nodesSelectedByDefault is false
###

echo "TEST: POST job/id/run should fail"


# now submit req
runurl="${APIURL}/job/${jobid2}/run"
params=""

# get listing
$CURL -H "$AUTHHEADER" -X POST ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#get execid

execcount=$($XMLSTARLET sel -T -t -v "/result/executions/@count" $DIR/curl.out)
execid=$($XMLSTARLET sel -T -t -v "/result/executions/execution/@id" $DIR/curl.out)

if [ "1" == "${execcount}" -a "" != "${execid}" ] ; then
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

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/executions/@count" $DIR/curl.out)
assert "1" "$itemcount" "execution count should be 1"
status=$($XMLSTARLET sel -T -t -v "/result/executions/execution/@status" $DIR/curl.out)
assert "failed" "$status" "execution status should be failed"

echo "OK"

###
# should succeeded if select any node and nodesSelectedByDefault is false
###

echo "TEST: POST job/id/run should fail"


# now submit req
runurl="${APIURL}/job/${jobid2}/run"
params=""
JSONDATA='{ "filter":"name: .*" }'

# get listing
$CURL -H "$AUTHHEADER" -X POST --data-binary "$JSONDATA" -H content-type:application/json \
  -H accept:application/xml ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#get execid

execcount=$($XMLSTARLET sel -T -t -v "/result/executions/@count" $DIR/curl.out)
execid=$($XMLSTARLET sel -T -t -v "/result/executions/execution/@id" $DIR/curl.out)

if [ "1" == "${execcount}" -a "" != "${execid}" ] ; then
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

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/executions/@count" $DIR/curl.out)
assert "1" "$itemcount" "execution count should be 1"
status=$($XMLSTARLET sel -T -t -v "/result/executions/execution/@status" $DIR/curl.out)
assert "succeeded" "$status" "execution status should be succeeded"

echo "OK"


###
# Specify option parameters in json
###

echo "TEST: POST job/id/run with JSON"


# now submit req
runurl="${APIURL}/job/${jobid}/run"
params=""
JSONDATA='{"options":{"opt1":"xyz","opt2":"def"}}'

# get listing
$CURL -H "$AUTHHEADER" -X POST --data-binary "$JSONDATA" -H content-type:application/json \
  -H accept:application/xml \
  ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#get execid

execcount=$($XMLSTARLET sel -T -t -v "/result/executions/@count" $DIR/curl.out)
execid=$($XMLSTARLET sel -T -t -v "/result/executions/execution/@id" $DIR/curl.out)

if [ "1" == "${execcount}" -a "" != "${execid}" ] ; then
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

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/executions/@count" $DIR/curl.out)
assert "1" "$itemcount" "execution count should be 1"
status=$($XMLSTARLET sel -T -t -v "/result/executions/execution/@status" $DIR/curl.out)
assert "succeeded" "$status" "execution status should be succeeded"

assert_xml_value "-opt1 xyz -opt2 def" "/result/executions/execution/argstring" $DIR/curl.out
assert_xml_value "xyz" "/result/executions/execution/job/options/option[@name='opt1']/@value" $DIR/curl.out
assert_xml_value "def" "/result/executions/execution/job/options/option[@name='opt2']/@value" $DIR/curl.out

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

execcount=$($XMLSTARLET sel -T -t -v "/result/executions/@count" $DIR/curl.out)
execid=$($XMLSTARLET sel -T -t -v "/result/executions/execution/@id" $DIR/curl.out)

if [ "1" == "${execcount}" -a "" != "${execid}" ] ; then
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

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/executions/@count" $DIR/curl.out)
assert "1" "$itemcount" "execution count should be 1"
status=$($XMLSTARLET sel -T -t -v "/result/executions/execution/@status" $DIR/curl.out)
assert "succeeded" "$status" "execution status should be succeeded"

assert_xml_value "-opt1 xyz -opt2 a" "/result/executions/execution/argstring" $DIR/curl.out
assert_xml_value "xyz" "/result/executions/execution/job/options/option[@name='opt1']/@value" $DIR/curl.out
assert_xml_value "a" "/result/executions/execution/job/options/option[@name='opt2']/@value" $DIR/curl.out

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

