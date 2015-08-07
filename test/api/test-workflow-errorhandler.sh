#!/bin/bash

#test workflow execution with error handler steps

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

project=$2
if [ "" == "$2" ] ; then
    project="test"
fi

createjob(){

  jobname=$1
  shift
  jobkeepgoing=$1
  shift
  ehkeepgoing=$1
  shift
  ehstepexec=$1
  shift
  wffinalstepexec=$1
  shift

  # job execution that fails
  args="echo step will fail; false"

  args2="$ehstepexec"

  args3="$wffinalstepexec"

  #escape the string for xml
  xmlargs=$($XMLSTARLET esc "$args")
  xmlargs2=$($XMLSTARLET esc "$args2")
  xmlargs3=$($XMLSTARLET esc "$args3")
  xmlproj=$($XMLSTARLET esc "$project")

  #produce job.xml content corresponding to the dispatch request
  cat > $DIR/temp.out <<END
<joblist>
   <job>
      <name>$jobname</name>
      <group>api-test/workflow-errorhandler</group>
      <description></description>
      <loglevel>INFO</loglevel>
      <context>
          <project>$xmlproj</project>
      </context>
      <dispatch>
        <threadcount>1</threadcount>
        <keepgoing>true</keepgoing>
      </dispatch>
      <sequence keepgoing="$jobkeepgoing">
        <command>
          <exec>$xmlargs</exec>
          <errorhandler keepgoingOnSuccess="$ehkeepgoing">
            <exec>$xmlargs2</exec>
          </errorhandler>
        </command>
        <command>
            <exec>$xmlargs3</exec>
        </command>
      </sequence>
   </job>
</joblist>

END

  # now submit req
  runurl="${APIURL}/project/$project/jobs/import"

  params=""

  # specify the file for upload with curl, named "xmlBatch"
  ulopts="-F xmlBatch=@$DIR/temp.out -X POST"

  # get listing
  docurl $ulopts ${runurl}?${params} > $DIR/curl.out
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
      exit 2
  fi
  echo $jobid
}


runjob(){
  jobid=$1
  shift
  ###
  # Run the chosen id, expect success message
  ###

  # now submit req
  runurl="${APIURL}/job/${jobid}/run"
  params=""

  # get listing
  docurl -X POST ${runurl}?${params} > $DIR/curl.out
  if [ 0 != $? ] ; then
      errorMsg "ERROR: failed query request"
      exit 2
  fi

  #test success result
  $SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

  #get execid

  execcount=$($XMLSTARLET sel -T -t -v "/result/executions/@count" $DIR/curl.out)
  execid=$($XMLSTARLET sel -T -t -v "/result/executions/execution/@id" $DIR/curl.out)

  if [ "1" != "${execcount}" -o "" == "${execid}" ] ; then
      errorMsg "FAIL: expected run success message for execution id. (count: ${execcount}, id: ${execid})"
      exit 2
  fi

  echo $execid
}




waitexecstatus(){
  execid=$1
  shift
  expectstatus=$1
  shift
  # sleep
  # 
  rd-queue follow -q -e $execid || fail "Waiting for $execid to finish"
  $SHELL $SRC_DIR/api-expect-exec-success.sh $execid $expectstatus || exit 2
}


###
# TEST: execution of job with keepgoing=true, errorhandler step succeeds
###

echo "TEST: workflow error handler: keepgoing=true, handler succeeds"

testoutputfile=/tmp/testWorkflowEHOutput1.out

jobid=$( createjob "recovery handler1" "true" "false" "echo handler executed successfully >> $testoutputfile" \
  "echo final workflow step >> $testoutputfile")

if [ 0 != $? ] ; then
  fail "Failed to create job"
fi

execid=$( runjob $jobid )

waitexecstatus $execid "succeeded"

#then test that errorhandler output was correct

if [ ! -f $testoutputfile ] ; then
    errorMsg "FAIL: expected outputfile to be created: $testoutputfile"
    exit 2
fi

grep -q 'handler executed successfully' $testoutputfile

if [ $? != 0 ] ; then
    errorMsg "FAIL: outputfile content not correct, did not see errorhandler output: $testoutputfile"
    exit 2
fi
grep -q 'final workflow step' $testoutputfile

if [ $? != 0 ] ; then
    errorMsg "FAIL: outputfile content not correct, did not see final step output: $testoutputfile"
    exit 2
fi

echo "OK"

rm $testoutputfile || cat /dev/null > $testoutputfile
rm $DIR/curl.out


###
# TEST: execution of job with keepgoing=true, errorhandler step fails
###

echo "TEST: workflow error handler: keepgoing=true, handler fails"

testoutputfile=/tmp/testWorkflowEHOutput2.out

jobid=$( createjob "recovery handler1" "true" "false" "echo handler executed successfully >> $testoutputfile ; false " \
  "echo final workflow step >> $testoutputfile")

execid=$( runjob $jobid )

waitexecstatus $execid "failed"

#then test that errorhandler output was correct

if [ ! -f $testoutputfile ] ; then
    errorMsg "FAIL: expected outputfile to be created: $testoutputfile"
    exit 2
fi

grep -q 'handler executed successfully' $testoutputfile

if [ $? != 0 ] ; then
    errorMsg "FAIL: outputfile content not correct, did not see errorhandler output: $testoutputfile"
    exit 2
fi
grep -q 'final workflow step' $testoutputfile

if [ $? != 0 ] ; then
    errorMsg "FAIL: outputfile content not correct, did not see final step output: $testoutputfile"
    exit 2
fi

echo "OK"

rm $testoutputfile || cat /dev/null > $testoutputfile
rm $DIR/curl.out



###
# TEST: execution of job with keepgoing=false, errorhandler step fails
###

echo "TEST: workflow error handler: keepgoing=false, handler fails"

testoutputfile=/tmp/testWorkflowEHOutput3.out

jobid=$( createjob "recovery handler1" "false" "false" "echo handler executed successfully >> $testoutputfile ; false " \
  "echo final workflow step >> $testoutputfile")

execid=$( runjob $jobid )

waitexecstatus $execid "failed"

#then test that errorhandler output was correct

if [ ! -f $testoutputfile ] ; then
    errorMsg "FAIL: expected outputfile to be created: $testoutputfile"
    exit 2
fi

grep -q 'handler executed successfully' $testoutputfile

if [ $? != 0 ] ; then
    errorMsg "FAIL: outputfile content not correct, did not see errorhandler output: $testoutputfile"
    exit 2
fi
grep -q 'final workflow step' $testoutputfile

if [ $? == 0 ] ; then
    errorMsg "FAIL: outputfile content not correct, should not see final step output: $testoutputfile"
    exit 2
fi

echo "OK"

rm $testoutputfile || cat /dev/null > $testoutputfile
rm $DIR/curl.out

###
# TEST: execution of job with keepgoing=false, errorhandler step succeeds (keepgoingOnSuccess=false)
###

echo "TEST: workflow error handler: keepgoing=false, handler succeeds, keepgoingOnSuccess=false"

testoutputfile=/tmp/testWorkflowEHOutput3.out

jobid=$( createjob "recovery handler1" "false" "false" "echo handler executed successfully >> $testoutputfile " \
  "echo final workflow step >> $testoutputfile")

execid=$( runjob $jobid )

waitexecstatus $execid "failed"

#then test that errorhandler output was correct

if [ ! -f $testoutputfile ] ; then
    errorMsg "FAIL: expected outputfile to be created: $testoutputfile"
    exit 2
fi

grep -q 'handler executed successfully' $testoutputfile

if [ $? != 0 ] ; then
    errorMsg "FAIL: outputfile content not correct, did not see errorhandler output: $testoutputfile"
    exit 2
fi
grep -q 'final workflow step' $testoutputfile

if [ $? == 0 ] ; then
    errorMsg "FAIL: outputfile content not correct, should not see final step output: $testoutputfile"
    exit 2
fi

echo "OK"

rm $testoutputfile || cat /dev/null > $testoutputfile
rm $DIR/curl.out
###
# TEST: execution of job with keepgoing=false, errorhandler step succeeds (keepgoingOnSuccess=true)
###

echo "TEST: workflow error handler: keepgoing=false, handler succeeds, keepgoingOnSuccess=true"

testoutputfile=/tmp/testWorkflowEHOutput3.out

jobid=$( createjob "recovery handler1" "false" "true" "echo handler executed successfully >> $testoutputfile " \
  "echo final workflow step >> $testoutputfile")

execid=$( runjob $jobid )

waitexecstatus $execid "succeeded"

#then test that errorhandler output was correct

if [ ! -f $testoutputfile ] ; then
    errorMsg "FAIL: expected outputfile to be created: $testoutputfile"
    exit 2
fi

grep -q 'handler executed successfully' $testoutputfile

if [ $? != 0 ] ; then
    errorMsg "FAIL: outputfile content not correct, did not see errorhandler output: $testoutputfile"
    exit 2
fi
grep -q 'final workflow step' $testoutputfile

if [ $? != 0 ] ; then
    errorMsg "FAIL: outputfile content not correct, should not see final step output: $testoutputfile"
    exit 2
fi

echo "OK"

rm $testoutputfile || cat /dev/null > $testoutputfile
rm $DIR/curl.out
