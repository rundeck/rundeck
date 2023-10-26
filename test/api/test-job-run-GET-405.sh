#!/bin/bash

#test  /api/13/job/{id}/run

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
</joblist>

END

jobid=$(uploadJob "$DIR/temp.out" "$project"  1 "")
if [ 0 != $? ] ; then
  errorMsg "failed job upload"
  exit 2
fi

###
# Run the chosen id, expect 405 message
###

execargs="-opt2+a"

ENDPOINT="${RDURL}/api/14/job/${jobid}/run"
ACCEPT=application/json
EXPECT_STATUS=405
METHOD=GET
PARAMS="argString=${execargs}"

test_begin "TEST: GET /api/14/job/id/run should fail"

# get listing
api_request $ENDPOINT $DIR/curl.out

echo "OK"

rm $DIR/curl.out

