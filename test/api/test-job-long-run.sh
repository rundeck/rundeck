#!/bin/bash

#test  Ensure jobs are not holding up database connections

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

###
# setup: create a new job and acquire the ID
###

# job exec
args="sleep 7"

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

# now submit req
runurl="${APIURL}/project/$project/jobs/import"

params="dupeOption=update"

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
jobid=$($XMLSTARLET sel -T -t -v "/result/succeeded/job[1]/id" $DIR/curl.out)
ref_jobid=$($XMLSTARLET sel -T -t -v "/result/succeeded/job[2]/id" $DIR/curl.out)
if [ "2" != "$succount" -o "" == "$jobid" ] ; then
    errorMsg  "Upload was not successful."
    exit 1
fi


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

    execcount=$($XMLSTARLET sel -T -t -v "/result/executions/@count" $DIR/curl.out)
    execid=$($XMLSTARLET sel -T -t -v "/result/executions/execution/@id" $DIR/curl.out)

    if [ "1" == "${execcount}" -a "" != "${execid}" ] ; then
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
    itemcount=$($XMLSTARLET sel -T -t -v "/result/executions/@count" $DIR/curl.out)
    assert "1" "$itemcount" "execution count should be 1"
    status=$($XMLSTARLET sel -T -t -v "/result/executions/execution/@status" $DIR/curl.out)
    assert "succeeded" "$status" "execution status should be succeeded"

    echo "OK"

    rm $DIR/curl.out
}

runJob $jobid

runJob $ref_jobid
