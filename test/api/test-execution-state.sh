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

runurl="${APIURL}/run/command"
proj="test"
params="project=${proj}&exec=echo+testing+execution+api"

# get listing
docurl -X POST ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#select id

execid=$($XMLSTARLET sel -T -t -v "/result/execution/@id" $DIR/curl.out)

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


echo "TEST: ${runurl}?${params} (xml) ..."
sleep 2

# get listing
docurl -H 'Accept:application/xml' ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi
    
$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "count(/result/executionState)" $DIR/curl.out)
assert "1" "$itemcount" "execution state count should be 1"
assert_xml_value "$localnode" "/result/executionState[@id='${execid}']/targetNodes/nodes/node/@name" $DIR/curl.out

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
jobid=$($XMLSTARLET sel -T -t -v "/result/succeeded/job[3]/id" $DIR/curl.out)

if [ "3" != "$succount" -o "" == "$jobid" ] ; then
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
execargs="-opt1 foobar"

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
#.
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

####
# Test: get execution state in XML
####

# now submit req
runurl="${APIURL}/execution/${execid}/state"
params=""


echo "TEST: ${runurl}?${params} ..."
sleep 2
# get listing
docurl -H 'Accept:application/xml' ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check execution state
itemcount=$($XMLSTARLET sel -T -t -v "count(/result/executionState)" $DIR/curl.out)
assert "1" "$itemcount" "execution state count should be 1"
assert_xml_value "$localnode" "/result/executionState[@id='${execid}']/targetNodes/nodes/node/@name" $DIR/curl.out
# Regression test for #2268 - check for well-formed node states
assert_xml_value "SUCCEEDED" \
    "/result/executionState[@id='${execid}']/steps/step[@id=1]/nodeStates/nodeState[@name='${localnode}']/executionState" \
    $DIR/curl.out

echo "OK"

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

