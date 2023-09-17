#!/bin/bash

# use api V44
API_VERSION=44

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh
args="echo hello there"

projectName=""
jobName=""

generate_projectName_and_jobName(){
    projectName="project-$RANDOM"
    jobName="job-$RANDOM"
}

create_job(){
    projname=$1
    jobname=$2
    xmljob=$($XMLSTARLET esc "$jobname")

    cat > $DIR/job_create.post <<END
<joblist>
   <job>
      <name>$xmljob</name>
      <group>api-test</group>
      <description></description>
      <loglevel>INFO</loglevel>
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

    jobid=$(uploadJob "$DIR/job_create.post" "$projname"  1 "")
    if [ 0 != $? ] ; then
      errorMsg "failed job upload"
      exit 2
    fi
    echo $jobid
}

create_proj(){
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


disable_execution_bulk(){
    jobset=$1

    runurl="${APIURL}/jobs/execution/disable"
    params="idlist=$jobset"

    docurl -X POST  ${runurl}?${params} > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed POST request (disable execution)"
        exit 2
    fi
    assert_xml_value 'true' '//toggleExecution/@allsuccessful' $DIR/curl.out
}
enable_execution_bulk(){
    jobset=$1

    runurl="${APIURL}/jobs/execution/enable"
    params="idlist=$jobset"

    docurl -X POST  ${runurl}?${params} > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed POST request (enable execution)"
        exit 2
    fi
    assert_xml_value 'true' '//toggleExecution/@allsuccessful' $DIR/curl.out
}

execute_job(){
    jobname=$1
    succeed=$2

    runurl="${APIURL}/job/$jobname/run"
    params=""

    # get listing
    docurl -D $DIR/headers.out -X POST -H Accept:application/xml ${runurl}?${params} > $DIR/curl.out

    if [ $succeed == 0 ] ; then
      grep "HTTP/1.1 200" -q $DIR/headers.out
      okheader=$?
      if [ 0 != $okheader ] ; then
          errorMsg "FAIL: Response was not $code"
          grep 'HTTP/1.1' $DIR/headers.out
          exit 2
      fi
    else
        assert_xml_value 'true' '/result/@error' $DIR/curl.out
    fi

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

    assert $expectedcount $(xmlsel "//executions/@count" $DIR/curl.out) "Wrong number of executions"
}

####
# Test: when execution is on, job does execute
####

generate_projectName_and_jobName
create_proj $projectName
jobid1=$(create_job $projectName $jobName)
jobid2=$(create_job $projectName "${jobName}-2")
assert_job_execution_count $jobid1 "0"
assert_job_execution_count $jobid2 "0"
execute_job $jobid1 0
execute_job $jobid2 0
assert_job_execution_count $jobid1 "1"
assert_job_execution_count $jobid2 "1"

echo "TEST: bulk job execution disable"

disable_execution_bulk "${jobid1},${jobid2}"

execute_job $jobid1 1
execute_job $jobid2 1
assert_job_execution_count $jobid1 "1"
assert_job_execution_count $jobid2 "1"
echo "OK"

echo "TEST: bulk job execution enable"
enable_execution_bulk "${jobid1},${jobid2}"

execute_job $jobid1 0
execute_job $jobid2 0
assert_job_execution_count $jobid1 "2"
assert_job_execution_count $jobid2 "2"
echo "OK"

delete_proj $projectName


exit 0
