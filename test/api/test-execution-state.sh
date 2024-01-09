#!/bin/bash

#test output from /api/execution/{id}/state

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

###
# Setup: acquire local node name from RDECK_ETC/framework.properties#server.name
####
localnode=$(grep 'framework.server.name' $RDECK_ETC/framework.properties | sed 's/framework.server.name = //')

if [ -z "${localnode}" ] ; then
    errorMsg "FAIL: Unable to determine framework.server.name from $RDECK_ETC/framework.properties"
    exit 2
fi

####
# Setup: create simple adhoc command execution to provide execution ID.
####

proj="test"
runurl="${APIURL}/project/${proj}/run/command"
params="exec=echo+testing+execution+api"

# get listing
docurl -X POST ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#select id
execid=$(jq -r ".execution.id" < $DIR/curl.out)

if [ -z "$execid" ] ; then
    errorMsg "FAIL: expected execution id"
    exit 2
fi

####
# Test:
####

# now submit req
runurl="${APIURL}/execution/${execid}/state"
params=""


echo "TEST: ${runurl}?${params} (json) ..."
sleep 2

# get listing
docurl -H 'Accept:application/json' ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#Check projects list
assert_json_not_null ".executionState" $DIR/curl.out
assert_json_value "$localnode" ".targetNodes[0]" $DIR/curl.out

echo "OK"

params=""

####
# Setup:
####

# job exec
project=$2
if [ "" == "$2" ] ; then
    project="test"
fi

#escape the string for xml
xmlproj=$($XMLSTARLET esc "$project")

#produce job.xml content corresponding to the dispatch request
cat > $DIR/temp.out <<END
<joblist>
   <job>
      <name>grandchild</name>
      <group>api-test/execution-state</group>
      <description></description>
      <loglevel>INFO</loglevel>
      <context>
          <project>$xmlproj</project>
          <options />
      </context>
      <dispatch>
        <threadcount>1</threadcount>
        <keepgoing>true</keepgoing>
      </dispatch>
      <sequence>
        <command>
          <exec>echo this is the grandchild job</exec>
        </command>
      </sequence>
   </job>

   <job>
      <name>child</name>
      <group>api-test/execution-state</group>
      <description></description>
      <loglevel>INFO</loglevel>
      <context>
          <project>$xmlproj</project>
          <options>
              <option name="opt1" value="testvalue" required="true"/>
          </options>
      </context>
      <dispatch>
        <threadcount>1</threadcount>
        <keepgoing>true</keepgoing>
      </dispatch>
      <sequence>
        <command>
          <exec>echo this is the child job</exec>
        </command>
        <command>
          <description>Run a child job</description>
          <jobref group="api-test/execution-state" name="grandchild" nodeStep="true" />
        </command>
      </sequence>
   </job>

   <job>
      <name>parent</name>
      <group>api-test/execution-state</group>
      <description></description>
      <loglevel>INFO</loglevel>
      <context>
          <project>$xmlproj</project>
          <options>
              <option name="opt1" value="testvalue" required="true"/>
          </options>
      </context>
      <dispatch>
        <threadcount>1</threadcount>
        <keepgoing>true</keepgoing>
      </dispatch>
      <sequence>
        <command>
          <exec>echo hello world</exec>
        </command>
        <command>
          <description>Run a child job</description>
          <jobref group="api-test/execution-state" name="child" nodeStep="true">
            <arg line="-opt1 testvalue" />
          </jobref>
        </command>
      </sequence>
   </job>
</joblist>

END

jobid=$(uploadJob "$DIR/temp.out" "$project" 3 "" ".succeeded[2].id")
if [ 0 != $? ] ; then
  errorMsg "failed job upload"
  exit 2
fi

###
# Run the chosen id, expect success message
###

echo "TEST: POST job/id/run should succeed"


execid=$(runjob "$jobid" "-opt1 foobar")

#wait for execution to complete

api_waitfor_execution $execid || fail "Failed waiting for execution $execid to complete"

# test execution status
#.
runurl="${APIURL}/execution/${execid}"

params=""

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#Check projects list
assert_json_value "succeeded" ".status" $DIR/curl.out

echo "OK"

runurl="${APIURL}/execution/${execid}/state"
params=""

####
# Test: get execution state in JSON
####

# now submit req
echo "TEST: ${runurl}?${params} (json) ..."

# get listing
docurl -H 'Accept:application/json' ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

assert_json_value "SUCCEEDED" ".steps[0].nodeStates[\"${localnode}\"].executionState" $DIR/curl.out

echo "OK"

