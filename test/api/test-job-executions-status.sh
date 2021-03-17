#!/bin/bash

#test  /api/job/{id}/executions

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

####
# create job that halts with a status message using the flow control plugin
####


project="test"

#escape the string for xml
xmlargs=$($XMLSTARLET esc "$args")
xmlproj=$($XMLSTARLET esc "$project")

#produce job.xml content corresponding to the dispatch request
cat > $DIR/temp.out <<END
<joblist>
   <job>
      <name>test custom job status query</name>
      <group>test/api/executions</group>
      <description>Test the /job/ID/executions API endpoint with custom status</description>
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
        <description>halt with custom status</description>
        <step-plugin type='flow-control'>
          <configuration>
            <entry key='fail' value='false' />
            <entry key='halt' value='true' />
            <entry key='status' value='test status code' />
          </configuration>
        </step-plugin>
      </command>
      </sequence>
   </job>
</joblist>

END
jobid=$(uploadJob "$DIR/temp.out" "$project" 1 "dupeOption=create")


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
    echo "OK"
else
    errorMsg "FAIL: expected run success message for execution id. (count: ${execcount}, id: ${execid})"
    exit 2
fi

# let job finish
sleep 10

###
# Test result of /job/ID/executions?status=failed is 1 list
###

echo "TEST: job/id/executions?status=test+status+code with 1 results"

# now submit req
runurl="${APIURL}/job/${jobid}/executions"
params="status=test+status+code"

# get listing
docurl  ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#verify 1 results

assert "1" $(xmlsel "//executions/@count" $DIR/curl.out) "Wrong number of executions"
assert "${execid}" $(xmlsel "//executions/execution/@id" $DIR/curl.out) "Wrong ID found"
echo "OK"
