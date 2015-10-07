#!/bin/bash

#test /api/jobs/import

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh
args="echo hello there"

projectName=""
jobName=""
jobId=""

generate_projectName_and_jobName(){
    projectName="project-$(date "+%s")"
    jobName="job-$(date "+%s")"
}

create_proj_and_job(){
    projname=$1
    jobname=$2

    xmlproj=$($XMLSTARLET esc "$projname")
    xmljob=$($XMLSTARLET esc "$jobname")

    cat > $DIR/proj_create.post <<END
<project>
    <name>$xmlproj</name>
    <description>description for $xmlproj</description>
    <config>
        <property key="test.property" value="test value"/>
    </config>
</project>
END

    runurl="${APIURL}/projects"

    # post (thus creating project)
    docurl -X POST -D $DIR/headers.out --data-binary @$DIR/proj_create.post -H Content-Type:application/xml ${runurl}?${params} > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed POST request"
        exit 2
    fi
    rm $DIR/proj_create.post
    assert_http_status 201 $DIR/headers.out

    #determine h:m:s to run, 10 seconds from now
    NDATES=$(date '+%s')
    NDATES=$(( $NDATES + 10 ))
    osname=$(uname)
    if [ "Darwin" = "$osname" ] ; then
        NDATE=$(date -r "$NDATES" '+%Y %m %d %H %M %S')
    else
        NDATE=$(date --date="@$NDATES" '+%Y %m %d %H %M %S')
    fi
    NY=$(echo $NDATE | cut -f 1 -d ' ')
    NMO=$(echo $NDATE | cut -f 2 -d ' ')
    ND=$(echo $NDATE | cut -f 3 -d ' ')
    NH=$(echo $NDATE | cut -f 4 -d ' ')
    NM=$(echo $NDATE | cut -f 5 -d ' ')
    NS=$(echo $NDATE | cut -f 6 -d ' ')

    cat > $DIR/job_create.post <<END
<joblist>
   <job>
      <name>$xmljob</name>
      <group>api-test</group>
      <description></description>
      <loglevel>INFO</loglevel>
      <context>
          <project>$xmlproj</project>
      </context>
      <dispatch>
        <threadcount>1</threadcount>
        <keepgoing>true</keepgoing>
      </dispatch>
      <schedule>
        <time hour='$NH' seconds='$NS' minute='$NM' />
        <month month='$NMO'  day='$ND' />
        <year year='$NY' />
      </schedule>
      <sequence>
        <command>
        <exec>echo hello there</exec>
        </command>
      </sequence>
   </job>
</joblist>
END

    # now submit req
    runurl="${APIURL}/project/$projname/jobs/import"
    params=""
    ulopts="-F xmlBatch=@$DIR/job_create.post"

    # get listing
    docurl $ulopts  ${runurl}?${params} > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed query request"
        exit 2
    fi

    jobId=$($XMLSTARLET sel -T -t -v "/result/succeeded/job/id" $DIR/curl.out)
    succount=$($XMLSTARLET sel -T -t -v "/result/succeeded/@count" $DIR/curl.out)

    if [ "1" != "$succount" -o "" == "$jobId" ] ; then
        errorMsg  "Upload was not successful."
        $XMLSTARLET sel -T -t -v "/result/failed" $DIR/curl.out
        exit 2
    fi
}

delete_proj(){
    projname=$1
    xmlproj=$($XMLSTARLET esc "$projname")

    runurl="${APIURL}/project/$projname"
    docurl -X DELETE  ${runurl} > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed DELETE request"
        exit 2
    fi
}

disable_schedule(){
    jobname=$1
    xmljob=$($XMLSTARLET esc "$jobname")

    runurl="${APIURL}/job/$jobname/schedule/disable"
    params=""

    docurl -X POST -H Content-Type:application/xml ${runurl}?${params} > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed POST request (disable execution)"
        exit 2
    fi
}

enable_schedule(){
    jobname=$1
    xmljob=$($XMLSTARLET esc "$jobname")

    runurl="${APIURL}/job/$jobname/schedule/enable"
    params=""

    docurl -X POST -H Content-Type:application/xml ${runurl}?${params} > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed POST request (disable execution)"
        exit 2
    fi
}

execute_job(){
    jobname=$1
    xmljob=$($XMLSTARLET esc "$jobname")

    runurl="${APIURL}/job/$jobname/run"
    params=""

    # get listing
    docurl -X POST -H Content-Type:application/xml ${runurl}?${params} > $DIR/curl.out

    # allow execution to end
    sleep 6
}

assert_job_execution_count(){
    jobname=$1
    xmljob=$($XMLSTARLET esc "$jobname")

    expectedcount=$2

    runurl="${APIURL}/job/${jobname}/executions"
    params=""

    # get listing
    docurl ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

    assert $expectedcount $(xmlsel "/result/executions/@count" $DIR/curl.out) "Wrong number of executions"
}

####
# Test: when schedule is on, job does execute
####
echo "TEST: when schedule is on, job does execute"
generate_projectName_and_jobName
create_proj_and_job $projectName $jobName
assert_job_execution_count $jobId "0"
sleep 10
assert_job_execution_count $jobId "1"
delete_proj $projectName

####
# Test: when schedule is off, job doesn't execute
####
echo "TEST: when schedule is off, job doesn't execute"
generate_projectName_and_jobName
create_proj_and_job $projectName $jobName
assert_job_execution_count $jobId "0"
disable_schedule $jobId
sleep 10
assert_job_execution_count $jobId "0"
delete_proj $projectName

####
# Test: when schedule is turned off and on again, job does execute
####
echo "TEST: when schedule is off, job doesn't execute"
generate_projectName_and_jobName
create_proj_and_job $projectName $jobName
assert_job_execution_count $jobId "0"
disable_schedule $jobId
enable_schedule $jobId
sleep 10
assert_job_execution_count $jobId "1"
delete_proj $projectName

exit 0
