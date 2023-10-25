#!/bin/bash

#test workflow execution with error handler steps

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

project=$2
if [ "" == "$2" ] ; then
    project="test"
fi

createjob(){
errorMsg "CREATE JOB: $1"
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

  jobid=$(uploadJob "$DIR/temp.out" "$project"  1 "")
  if [ 0 != $? ] ; then
    errorMsg "failed job upload"
    exit 2
  fi
  echo $jobid
}




waitexecstatus(){
  execid=$1
  shift
  expectstatus=$1
  shift
  # sleep
  #
  api_waitfor_execution $execid || fail "Waiting for $execid to finish"
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
