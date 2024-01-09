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

jobid=$(uploadJob "$DIR/temp.out" "$project"  4 "")
if [ 0 != $? ] ; then
  errorMsg "failed job upload"
  exit 2
fi

jobid="06ba3dce-ba4f-4964-8ac2-349c3a2267bd"

###
# Run the chosen id, expect success message
###

echo "TEST: POST job/id/run should succeed"


# run in parallel
execid=$(runjob "$jobid" "-opt2 a")
execid2=$(runjob "$jobid" "-opt2 a")
execid3=$(runjob "$jobid" "-opt2 a")

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

assert_json_value "succeeded" ".status" $DIR/curl.out


grep -i "Deadlock" $RDECK_BASE/var/log/service.log -q
found=$?
if [ 1 != $found ] ; then
    errorMsg "ERROR: Deadlock found on multiple executions"
    exit 2
fi
rm $DIR/curl.out
echo "OK"

