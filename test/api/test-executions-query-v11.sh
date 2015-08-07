#!/bin/bash
#set -x
#test output from /api/executions?queryparams

DIR=$(cd `dirname $0` && pwd)
API_VERSION=11
API_XML_NO_WRAPPER=true
source $DIR/include.sh

sleep 3

#determine date
#yyyy-MM-dd'T'HH:mm:ss'Z'
dformat="+%Y-%m-%dT%H:%M:%SZ"
startdate=$(date -u "$dformat")

# set up a few executions

runurl="${APIURL}/run/command"
proj="test"
params="project=${proj}&exec=echo+testing+adhoc+execution+query"

# get listing
docurl -X POST ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

execid1=$($XMLSTARLET sel -T -t -v "/execution/@id" $DIR/curl.out)
[ -n "$execid1" ] || fail "Didn't see execid"


runurl="${APIURL}/run/command"
proj="test"
params="project=${proj}&exec=echo+testing+adhoc+execution+query+should+fail;false"

# get listing
docurl -X POST ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

#sh $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2
execid2=$($XMLSTARLET sel -T -t -v "/execution/@id" $DIR/curl.out)
[ -n "$execid2" ] || fail "Didn't see execid"

###
# setup: create a new job and acquire the ID
###

uploadJob(){
    file=$1;shift

    # now submit req
    runurl="${APIURL}/jobs/import"

    params="dupeOption=update&project=${proj}"

    # specify the file for upload with curl, named "xmlBatch"
    ulopts="-F xmlBatch=@$file"

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
    jobid=$($XMLSTARLET sel -T -t -v "/result/succeeded/job/id" $DIR/curl.out)

    if [ "1" != "$succount" -o "" == "$jobid" ] ; then
        errorMsg  "Upload was not successful."
        exit 
    fi

    echo $jobid
}

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
      <uuid>api-v5-test-exec-query</uuid>
      <name>test exec query</name>
      <group>api-test/execquery</group>
      <description>A job to test the executions query API</description>
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

jobid=$(uploadJob "$DIR/temp.out")


#produce job.xml content corresponding to the dispatch request
cat > $DIR/temp.out <<END
<joblist>
   <job>
      <uuid>api-v5-test-exec-query2</uuid>
      <name>second test for exec query</name>
      <group>api-test/execquery</group>
      <description>A job to test the executions query API</description>
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

jobid2=$(uploadJob "$DIR/temp.out")

runJob(){
    jobid=$1;shift
    # now run the job
    runurl="${APIURL}/job/${jobid}/run"
    params=""
    execargs="-opt2 a"

    # get listing
    $CURL -H "$AUTHHEADER" --data-urlencode "argString=${execargs}" ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

    #sh $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

    #get execid

    execcount=$($XMLSTARLET sel -T -t -v "/executions/@count" $DIR/curl.out)
    execid=$($XMLSTARLET sel -T -t -v "/executions/execution/@id" $DIR/curl.out)

    if [ "1" != "${execcount}" -o "" == "${execid}" ] ; then
        errorMsg "FAIL: expected run success message for execution id. (count: ${execcount}, id: ${execid})"
        exit 2
    fi
    echo $execid
}

execid3=$(runJob $jobid)

execid4=$(runJob $jobid2)

#wait for executions to complete
rd-queue follow -q -e $execid1 || {
  errorMsg "Failed to wait for execution $execid1 to finish"
  exit 2
}
rd-queue follow -q -e $execid2 || {
  errorMsg "Failed to wait for execution $execid2 to finish"
  exit 2
}
rd-queue follow -q -e $execid3 || {
  errorMsg "Failed to wait for execution $execid3 to finish"
  exit 2
}
rd-queue follow -q -e $execid4 || {
  errorMsg "Failed to wait for execution $execid4 to finish"
  exit 2
}

###
# test execution queries
###

# now submit req
runurl="${APIURL}/executions"
proj=$2
if [ "" == "$2" ] ; then
    proj="test"
fi


testExecQuery(){

    desc=$1;shift
    xargs=$1;shift
    params="project=${proj}&${xargs}"

    # get listing
    docurl ${runurl}?${params} > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed query request for test: $desc"
        exit 2
    fi

    #sh $SRC_DIR/api-test-success.sh $DIR/curl.out || (echo "${runurl}?${params}"; exit 2)

    #Check projects list
    itemcount=$($XMLSTARLET sel -T -t -v "/executions/@count" $DIR/curl.out)
    #echo "$itemcount executions"
    expect=$1;shift
    if [ -n "${expect}" ] ; then 
        if [ "${expect}" != "$itemcount" ] ; then
            errorMsg "FAIL: expected ${expect} but saw $itemcount results for test: $desc: $xargs, url: ${runurl}?${params}"
            cat $DIR/curl.out
            exit 2
        fi
    else
        if [ "" == "$itemcount" -o "0" == "$itemcount" ] ; then
            errorMsg "FAIL: expected some but saw $itemcount results for test: $desc: $xargs, url ${runurl}?${params}"
            cat $DIR/curl.out
            exit 2
        fi
    fi
    echo "OK"

}

echo "TEST: /api/executions for project ${proj}..."

testExecQuery "Basic" ""

#begin
testName="begin"

echo "TEST: /api/executions?begin=$startdate for project ${proj}..."

testExecQuery $testName "begin=$startdate" "4"

fakedate=2213-05-08T01:05:19Z
echo "TEST: /api/executions?begin= (DNE) for project ${proj}..."

testExecQuery "$testName DNE" "begin=$fakedate" "0"

basequery="begin=$startdate"

#jobIdListFilter
testName="jobIdListFilter"

echo "TEST: /api/executions?jobIdListFilter=$jobid for project ${proj}..."

testExecQuery $testName "jobIdListFilter=$jobid&$basequery" "1"

echo "TEST: /api/executions?jobIdListFilter=$jobid for project ${proj}..."

testExecQuery "$testName 2" "jobIdListFilter=$jobid&jobIdListFilter=$jobid2&$basequery" "2"

echo "TEST: /api/executions?jobIdListFilter= (DNE) for project ${proj}..."

testExecQuery "$testName DNE" "jobIdListFilter=$jobid+DNE-ID&$basequery" "0"

#excludeJobIdListFilter
testName="excludeJobIdListFilter"

echo "TEST: /api/executions?excludeJobIdListFilter=$jobid for project ${proj}..."

testExecQuery "$testName job1" "excludeJobIdListFilter=$jobid&$basequery" "1"

echo "TEST: /api/executions?excludeJobIdListFilter=$jobid2 for project ${proj}..."

testExecQuery "$testName job2" "excludeJobIdListFilter=$jobid2&$basequery" "1"

echo "TEST: /api/executions?excludeJobIdListFilter=$jobid2 for project ${proj}..."

testExecQuery "$testName both" "excludeJobIdListFilter=$jobid2&excludeJobIdListFilter=$jobid&$basequery" "0"

echo "TEST: /api/executions?excludeJobIdListFilter= (DNE) for project ${proj}..."

testExecQuery "$testName DNE" "excludeJobIdListFilter=$jobid+DNE-ID&$basequery" "2"

#jobFilter
testName="jobFilter"

echo "TEST: /api/executions?jobFilter= for project ${proj}..."

testExecQuery $testName "jobFilter=test+exec+query&$basequery"

echo "TEST: /api/executions?jobFilter= (DNE) for project ${proj}..."

testExecQuery "$testName DNE" "jobFilter=test+exec+query+DNE&$basequery" "0"

#jobListFilter
testName="jobListFilter"

echo "TEST: /api/executions?jobListFilter= for project ${proj}..."

testExecQuery $testName "jobListFilter=api-test%2Fexecquery%2Ftest+exec+query&jobListFilter=api-test%2Fexecquery%2Fsecond+test+for+exec+query&$basequery" "2"

echo "TEST: /api/executions?jobListFilter= (DNE) for project ${proj}..."

testExecQuery "$testName DNE" "jobListFilter=api-test%2Fexecquery%2FDNE+second+test+for+exec+query&$basequery" "0"

#excludeJobListFilter
testName="excludeJobListFilter"

echo "TEST: /api/executions?excludeJobListFilter=(empty) for project ${proj}..."

testExecQuery $testName "excludeJobListFilter=api-test%2Fexecquery%2Ftest+exec+query&excludeJobListFilter=api-test%2Fexecquery%2Fsecond+test+for+exec+query&$basequery" "0"

echo "TEST: /api/executions?excludeJobListFilter= for project ${proj}..."

testExecQuery "$testName 1" "excludeJobListFilter=api-test%2Fexecquery%2Ftest+exec+query&$basequery" "1"

echo "TEST: /api/executions?excludeJobListFilter= (DNE) for project ${proj}..."

testExecQuery "$testName DNE" "excludeJobListFilter=api-test%2Fexecquery%2FDNE+second+test+for+exec+query&$basequery" "2"

#jobExactFilter

testName="jobExactFilter"

echo "TEST: /api/executions?jobExactFilter= for project ${proj}..."

testExecQuery $testName "jobExactFilter=test+exec+query&$basequery"

echo "TEST: /api/executions?jobExactFilter= (DNE) for project ${proj}..."

testExecQuery "$testName DNE" "jobExactFilter=test+exec+query+DNE&$basequery" "0"

#groupPath
testName="groupPath"

echo "TEST: /api/executions?groupPath= for project ${proj}..."

testExecQuery $testName "groupPath=api-test%2Fexecquery&$basequery"

echo "TEST: /api/executions?groupPath= (DNE) for project ${proj}..."

testExecQuery "$testName DNE" "groupPath=api-test%2Fexecquery%2FDNEGROUP&$basequery" "0"

#groupPathExact

testName="groupPathExact"

echo "TEST: /api/executions?groupPathExact= for project ${proj}..."

testExecQuery $testName "groupPathExact=api-test%2Fexecquery&$basequery"

echo "TEST: /api/executions?groupPathExact= (DNE) for project ${proj}..."

testExecQuery "$testName DNE" "groupPathExact=api-test%2Fexecquery%2FDNEGROUP&$basequery" "0"

#descFilter

testName="descFilter"

echo "TEST: /api/executions?descFilter= for project ${proj}..."

testExecQuery $testName "descFilter=executions+query+API&$basequery"

echo "TEST: /api/executions?descFilter= (DNE) for project ${proj}..."

testExecQuery "$testName DNE" "descFilter=DNE+description&$basequery" "0"

#userFilter

testName="userFilter"

echo "TEST: /api/executions?userFilter= for project ${proj}..."

testExecQuery $testName "userFilter=admin&$basequery"

echo "TEST: /api/executions?userFilter= (DNE) for project ${proj}..."

testExecQuery "$testName DNE" "userFilter=DNEUser&$basequery" "0"

#statusFilter

testName="statusFilter"

echo "TEST: /api/executions?statusFilter= for project ${proj}..."

testExecQuery $testName "statusFilter=succeeded&jobIdListFilter=$jobid&$basequery"

echo "TEST: /api/executions?statusFilter=aborted for project ${proj}..."

testExecQuery "$testName aborted" "statusFilter=aborted&jobIdListFilter=$jobid&$basequery" "0"


#adhoc

testName="adhoc"

echo "TEST: /api/executions?adhoc=true for project ${proj}..."

testExecQuery $testName "adhoc=true&$basequery" "2"

echo "TEST: /api/executions?adhoc=false for project ${proj}..."

testExecQuery "$testName false" "adhoc=false&$basequery" "2"



#rm $DIR/curl.out

