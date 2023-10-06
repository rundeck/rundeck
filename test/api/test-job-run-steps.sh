#!/bin/bash

#test  /api/job/{id}/run
set -e


DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

###
# scripts to test with
###

SCRIPT_FILE_1=$DIR/job-run-steps-test-script1.txt
SCRIPT_FILE_2=$DIR/job-run-steps-test-script2.txt
DOS_LINE_SCRIPT=`cat $SCRIPT_FILE_2`
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
      <name>test job</name>
      <group>api-test/job-run-steps</group>
      <description></description>
      <loglevel>INFO</loglevel>
      <context>
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
         <command>
        <scriptargs>\${option.opt2}</scriptargs>
        <script><![CDATA[#!/bin/bash

echo "option opt1: \$RD_OPTION_OPT1"
echo "option opt1: @option.opt1@"
echo "node: @node.name@"
echo "option opt2: \$1"]]></script>
      </command>
         <command>
        <scriptargs>\${option.opt2}</scriptargs>
        <script><![CDATA[$DOS_LINE_SCRIPT]]></script>
      </command>
      <command>
        <jobref name='secondary job' group='api-test/job-run-steps'>
          <arg line='-opt1 asdf -opt2 asdf2' />
        </jobref>
      </command>
      <command>
        <scriptfile>$SCRIPT_FILE_1</scriptfile>
        <scriptargs />
      </command>
      </sequence>
   </job>
   <job>
      <name>secondary job</name>
      <group>api-test/job-run-steps</group>
      <description></description>
      <loglevel>INFO</loglevel>
      <context>
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
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed job xml generation"
    exit 2
fi
jobid=$(uploadJob "$DIR/temp.out" "$project"  2 "" )
if [ 0 != $? ] ; then
  errorMsg "failed job upload"
  exit 2
fi


###
# Run the chosen id, expect success message
###

echo "TEST: job/id/run should succeed"


execid=$(runjob "$jobid" "-opt2 a")

#wait for execution to complete
#
echo "TEST: execution ${execid} should succeed"

api_waitfor_execution $execid || fail "Failed waiting for execution $execid to complete"

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

echo "TEST: execution ${execid} output"


# now submit req
runurl="${APIURL}/execution/${execid}/output.text"

doff=0
ddone="false"
dlast=0
dmax=20
dc=0
OUTFILE=$DIR/job-run-steps-test-observed.output
TESTFILE=$DIR/job-run-steps-test-expected.output
nodename=$(xmlsel "/project/node[1]/@name" $RDECK_BASE/projects/test/etc/resources.xml)

cat > $TESTFILE <<END
hello there
option opt1: testvalue
option opt1: testvalue
node: $nodename
option opt2: a
this is script 2, opt1 is testvalue
hello there
this is script 1, opt1 is testvalue
END
#statements
params="offset=0"

# get listing
docurl ${runurl}?${params} > $OUTFILE
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi
diff -q $TESTFILE $OUTFILE
if [ 0 != $? ] ; then
    errorMsg "ERROR: Expected output was different"
    exit 2
fi
echo "OK"

rm $TESTFILE
rm $OUTFILE
