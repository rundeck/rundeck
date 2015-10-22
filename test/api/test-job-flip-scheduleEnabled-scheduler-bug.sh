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
        <month month='*' />
        <time hour='23' minute='22' seconds='0' />
        <weekday day='MON,SUN,THU,TUE,WED' />
        <year year='*' />
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

check_schedule_contents(){
    projname=$1
    xmlproj=$($XMLSTARLET esc "$projname")

    runurl="${APIURL}/jobs/export?project=${projname}&format=xml"
    params=""

    # get listing
    docurl ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

    assert 1 $(xmlsel "count(/joblist/job/schedule/month)" $DIR/curl.out) "can't find tag job/schedule/month"
    assert 1 $(xmlsel "count(/joblist/job/schedule/time)" $DIR/curl.out) "can't find tag job/schedule/time"
    assert 1 $(xmlsel "count(/joblist/job/schedule/year)" $DIR/curl.out) "can't find tag job/schedule/year"
    assert 1 $(xmlsel "count(/joblist/job/schedule/weekday)" $DIR/curl.out) "can't find tag job/schedule/weekday"
}

####
# Test: when schedule is on, job does execute
####
echo "TEST: when schedule is flipped, job remains scheduled"
generate_projectName_and_jobName
create_proj_and_job $projectName $jobName
check_schedule_contents $projectName
disable_schedule $jobId
check_schedule_contents $projectName
enable_schedule $jobId
check_schedule_contents $projectName

delete_proj $projectName

exit 0
