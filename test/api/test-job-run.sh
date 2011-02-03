#!/bin/bash

#test  /api/job/{id}/run

errorMsg() {
   echo "$*" 1>&2
}

DIR=$(cd `dirname $0` && pwd)

# accept url argument on commandline, if '-' use default
url="$1"
if [ "-" == "$1" ] ; then
    url='http://localhost:4440/api'
fi
apiurl="${url}/api"

VERSHEADER="X-RUNDECK-API-VERSION: 1.2"

# curl opts to use a cookie jar, and follow redirects, showing only errors
CURLOPTS="-s -S -L -c $DIR/cookies -b $DIR/cookies"
CURL="curl $CURLOPTS"


XMLSTARLET=xml

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
      <name>cli job</name>
      <group>api-test/job-run</group>
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
</joblist>

END

# now submit req
runurl="${apiurl}/jobs/import"

params=""

# specify the file for upload with curl, named "xmlBatch"
ulopts="-F xmlBatch=@$DIR/temp.out"

# get listing
$CURL $ulopts --header "$VERSHEADER" ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#test curl.out for valid xml
$XMLSTARLET val -w $DIR/curl.out > /dev/null 2>&1
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response was not valid xml"
    exit 2
fi

#test for expected /joblist element
$XMLSTARLET el $DIR/curl.out | grep -e '^result' -q
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response did not contain expected result"
    exit 2
fi

# job list query doesn't wrap result in common result wrapper
#If <result error="true"> then an error occured.
waserror=$($XMLSTARLET sel -T -t -v "/result/@error" $DIR/curl.out)
if [ "true" == "$waserror" ] ; then
    errorMsg "Server reported an error: "
    $XMLSTARLET sel -T -t -v "/result/error/message" -n  $DIR/curl.out
    exit 2
fi

#result will contain list of failed and succeeded jobs, in this
#case there should only be 1 failed or 1 succeeded since we submit only 1

succount=$($XMLSTARLET sel -T -t -v "/result/succeeded/@count" $DIR/curl.out)
jobid=$($XMLSTARLET sel -T -t -v "/result/succeeded/job/id" $DIR/curl.out)

if [ "1" != "$succount" -o "" == "$jobid" ] ; then
    errorMsg  "Upload was not successful."
    exit 
fi


###
# Run the chosen id, expect success message
###

echo "TEST: job/id/run should succeed"


# now submit req
runurl="${apiurl}/job/${jobid}/run"
params=""

# get listing
$CURL --header "$VERSHEADER"  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#test curl.out for valid xml
$XMLSTARLET val -w $DIR/curl.out > /dev/null 2>&1
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response was not valid xml"
    exit 2
fi

#test for expected /result element
$XMLSTARLET el $DIR/curl.out | grep -e '^result' -q
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response did not contain expected result"
    exit 2
fi

#expect success
waserror=$($XMLSTARLET sel -T -t -v "/result/@error" $DIR/curl.out)
wassucc=$($XMLSTARLET sel -T -t -v "/result/@success" $DIR/curl.out)
sucmsg=$($XMLSTARLET sel -T -t -v "/result/success/message" $DIR/curl.out)
if [ "true" == "$waserror" ] ; then
    errorMsg "FAIL: error result"
    $XMLSTARLET sel -T -t -v "/result/error/message" -n  $DIR/curl.out
    exit 2
fi
if [ "true" != "$wassucc" ] ; then
    errorMsg "FAIL: expected success"
    exit 2
fi

#get execid

execid=$($XMLSTARLET sel -T -t -v "/result/succeeded/execution/id" $DIR/curl.out)

if [ "" != "${execid}" -a "Execution started: ${execid}" == "$sucmsg" ] ; then
    echo "OK"
else
    errorMsg "FAIL: expected delete success message"
    exit 2
fi

rm $DIR/curl.out

