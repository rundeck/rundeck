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
    <context>
      <options preserveOrder='true'>
        <option name='maxWaitTimeSecs' value='0' />
      </options>
    </context>
    <defaultTab>summary</defaultTab>
    <description></description>
    <executionEnabled>true</executionEnabled>
    <id>3cce5f70-71aa-4e6c-b99e-9e866732448a</id>
    <loglevel>INFO</loglevel>
    <multipleExecutions>true</multipleExecutions>
    <name>job_c</name>
    <nodeFilterEditable>false</nodeFilterEditable>
    <scheduleEnabled>true</scheduleEnabled>
    <sequence keepgoing='false' strategy='node-first'>
      <command>
        <script><![CDATA[sleep @option.maxWaitTimeSecs@]]></script>
        <scriptargs />
      </command>
      <command>
        <exec>echo "regular job before parallel"</exec>
      </command>
    </sequence>
    <uuid>3cce5f70-71aa-4e6c-b99e-9e866732448a</uuid>
  </job>
  <job>
    <defaultTab>summary</defaultTab>
    <description></description>
    <executionEnabled>true</executionEnabled>
    <id>7d6d0958-7987-4a35-9ec3-7720f0985ae4</id>
    <loglevel>INFO</loglevel>
    <multipleExecutions>true</multipleExecutions>
    <name>job_d</name>
    <nodeFilterEditable>false</nodeFilterEditable>
    <scheduleEnabled>true</scheduleEnabled>
    <sequence keepgoing='false' strategy='parallel'>
      <command>
        <jobref name='job_c' nodeStep='true'>
          <arg line='-maxWaitTimeSecs 20 -oldmaxWaitTimeSecs 2100' />
        </jobref>
      </command>
      <command>
        <jobref name='job_c' nodeStep='true'>
          <arg line='-maxWaitTimeSecs 60 -oldmaxWaitTimeSecs 2400' />
        </jobref>
      </command>
    </sequence>
    <uuid>7d6d0958-7987-4a35-9ec3-7720f0985ae4</uuid>
  </job>
  <job>
    <defaultTab>summary</defaultTab>
    <description></description>
    <executionEnabled>true</executionEnabled>
    <id>165ef9b9-61dc-470c-91aa-3f6dc248249d</id>
    <loglevel>INFO</loglevel>
    <multipleExecutions>true</multipleExecutions>
    <name>job_b</name>
    <nodeFilterEditable>false</nodeFilterEditable>
    <scheduleEnabled>true</scheduleEnabled>
    <sequence keepgoing='false' strategy='node-first'>
      <command>
        <jobref name='job_c' nodeStep='true' />
      </command>
      <command>
        <jobref name='job_d' nodeStep='true' />
      </command>
    </sequence>
    <uuid>165ef9b9-61dc-470c-91aa-3f6dc248249d</uuid>
  </job>
  <job>
    <defaultTab>summary</defaultTab>
    <description></description>
    <executionEnabled>true</executionEnabled>
    <id>06ba3dce-ba4f-4964-8ac2-349c3a2267bd</id>
    <loglevel>INFO</loglevel>
    <multipleExecutions>true</multipleExecutions>
    <name>job_a</name>
    <nodeFilterEditable>false</nodeFilterEditable>
    <scheduleEnabled>true</scheduleEnabled>
    <sequence keepgoing='false' strategy='node-first'>
      <command>
        <exec>echo "start job_a"</exec>
      </command>
      <command>
        <jobref name='job_b' nodeStep='true' />
      </command>
    </sequence>
    <uuid>06ba3dce-ba4f-4964-8ac2-349c3a2267bd</uuid>
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
jobid="06ba3dce-ba4f-4964-8ac2-349c3a2267bd"
#$($XMLSTARLET sel -T -t -v "/result/succeeded/job/id" $DIR/curl.out)

if [ "4" != "$succount" -o "" == "$jobid" ] ; then
    errorMsg  "Upload was not successful."
    exit 2
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
$CURL -H "$AUTHHEADER" -X POST  ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

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

#running in parallel the same job to force concurrency.
$CURL -H "$AUTHHEADER" -X POST  ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"
$CURL -H "$AUTHHEADER" -X POST  ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"
$CURL -H "$AUTHHEADER" -X POST  ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

#wait for execution to complete
echo "TEST: POST job/id/run should succeed $execid"
api_waitfor_execution $execid true 40 || fail "Failed waiting for execution $execid to complete"

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


grep -i "Deadlock" $RDECK_BASE/var/log/service.log -q
found=$?
if [ 1 != $found ] ; then
    errorMsg "ERROR: Deadlock found on multiple executions"
    exit 2
fi
rm $DIR/curl.out
echo "OK"

