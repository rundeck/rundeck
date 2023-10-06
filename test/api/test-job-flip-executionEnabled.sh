#!/bin/bash

#set -e

# use api V44
API_VERSION=44

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh
args="echo hello there"

projectName=""
jobName=""
jobId=""

generate_projectName_and_jobName(){
    projectName="project-$RANDOM"
    jobName="job-$RANDOM"
}

create_proj_and_job(){
    projname=$1
    jobname=$2

    xmljob=$($XMLSTARLET esc "$jobname")

    create_project "$projname"

    cat > $DIR/job_create.post <<END
<joblist>
   <job>
      <name>$xmljob</name>
      <group>api-test</group>
      <description></description>
      <loglevel>INFO</loglevel>
      <context>
          <project>$projname</project>
      </context>
      <dispatch>
        <threadcount>1</threadcount>
        <keepgoing>true</keepgoing>
      </dispatch>
      <sequence>
        <command>
        <exec>echo hello there</exec>
        </command>
      </sequence>
   </job>
</joblist>
END

    jobId=$(uploadJob "$DIR/job_create.post" "$projname"  1 "")
    if [ 0 != $? ] ; then
      errorMsg "failed job upload"
      exit 2
    fi
}


disable_execution(){
    jobname=$1
    xmljob=$($XMLSTARLET esc "$jobname")

    runurl="${APIURL}/job/$jobname/execution/disable"
    params=""

    docurl -X POST  ${runurl}?${params} > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed POST request (disable execution)"
        exit 2
    fi
}

enable_execution(){
    jobname=$1
    xmljob=$($XMLSTARLET esc "$jobname")

    runurl="${APIURL}/job/$jobname/execution/enable"
    params=""

    docurl -X POST ${runurl}?${params} > $DIR/curl.out
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
    docurl -X POST  ${runurl}?${params} > $DIR/curl.out

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

    assert_json_value $expectedcount ".executions | length" $DIR/curl.out
}

####
# Test: when execution is on, job does execute
####
echo "TEST: when schedule is on, job does execute"
generate_projectName_and_jobName
create_proj_and_job $projectName $jobName
assert_job_execution_count $jobId "0"
execute_job $jobId
assert_job_execution_count $jobId "1"
delete_proj $projectName

####
# Test: when execution is off, job doesn't execute
####
echo "TEST: when schedule is off, job doesn't execute"
generate_projectName_and_jobName
create_proj_and_job $projectName $jobName
assert_job_execution_count $jobId "0"
disable_execution $jobId
execute_job $jobId
assert_job_execution_count $jobId "0"
delete_proj $projectName

####
# Test: when execution is off and then on again, job does execute
####
echo "TEST: when execution is off and then on again, job does execute"
generate_projectName_and_jobName
create_proj_and_job $projectName $jobName
assert_job_execution_count $jobId "0"
disable_execution $jobId
execute_job $jobId
assert_job_execution_count $jobId "0"
enable_execution $jobId
execute_job $jobId
assert_job_execution_count $jobId "1"
delete_proj $projectName

exit 0
