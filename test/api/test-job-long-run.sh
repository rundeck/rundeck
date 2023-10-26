#!/bin/bash

#test  Ensure jobs are not holding up database connections

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

###
# setup: create a new job and acquire the ID
###

# job exec
args="sleep 12"

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
      <name>Long Run Job</name>
      <group>api-test/job-run</group>
      <uuid>db9a5f0d</uuid>
      <description></description>
      <loglevel>INFO</loglevel>
      <context>
          <project>$xmlproj</project>
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
      <name>Long Run Wrapper</name>
      <group>api-test/job-run</group>
      <uuid>r2d2</uuid>
      <description/>
      <executionEnabled>true</executionEnabled>
      <loglevel>INFO</loglevel>
       <context>
          <project>$xmlproj</project>
      </context>
      <nodeFilterEditable>false</nodeFilterEditable>
      <sequence keepgoing="false" strategy="parallel">
         <command>
             <jobref name="Long Run Job">
               <uuid>db9a5f0d</uuid>
             </jobref>
         </command>
         <command>
             <jobref name="Long Run Job" nodeStep="true">
               <uuid>db9a5f0d</uuid>
             </jobref>
         </command>
      </sequence>
   </job>
</joblist>

END

jobid=$(uploadJob "$DIR/temp.out" "$project"  2 "dupeOption=update" ".succeeded[0].id")
if [ 0 != $? ] ; then
  errorMsg "failed job upload"
  exit 2
fi
ref_jobid=$(jq -r ".succeeded[1].id" < $DIR/curl.out)


runJob() {
    ###
    # Run the chosen id, expect success message
    ###

    local jobid=${1:?Must supply job id}

    echo "TEST: POST job/id/run should succeed"


    # now submit req
    runurl="${APIURL}/job/${jobid}/run"
    params=""

    # get listing
    $CURL -H "$AUTHHEADER" -X POST ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

    $SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

    #get execid

    execid=$(jq -r ".id" < $DIR/curl.out)

    if [  "" != "${execid}" ] ; then
        :
    else
        errorMsg "FAIL: expected run success message for execution id. (count: ${execcount}, id: ${execid})"
        exit 2
    fi

    #wait for execution to complete

    api_waitfor_execution $execid 3 || fail "Failed waiting for execution $execid to complete"

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

    echo "OK"

    rm $DIR/curl.out
}

runJob $jobid

runJob $ref_jobid
