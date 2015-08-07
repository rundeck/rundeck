#!/bin/bash

#test DELETE for /api/job/{id}


DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

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
      <group>api-test/job-delete</group>
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
runurl="${APIURL}/project/$project/jobs/import"

params=""

# specify the file for upload with curl, named "xmlBatch"
ulopts="-F xmlBatch=@$DIR/temp.out"

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
    exit 
fi


###
# DELETE the chosen id, expect success message
###

echo "TEST: DELETE job should succeed"


# now submit req
runurl="${APIURL}/job/${jobid}"
params=""

#dont' allow redirects, remove -L
if [ -n "$RDAUTH" ] ; then 
    CURLOPTS="-s -S"
else
    CURLOPTS="-s -S -c $DIR/cookies -b $DIR/cookies"
fi
CURL="curl $CURLOPTS"

# get listing
$CURL -D $DIR/headers.out -H "$AUTHHEADER" -X DELETE ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

# allow 204 no content response
if ! grep 'HTTP/1.1 204' $DIR/headers.out ; then
#test success result
  $SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out "Job was successfully deleted: [${jobid}] api-test/job-delete/cli job" || exit 2
fi
echo "OK"
rm $DIR/headers.out

###
# Get the chosen id, expect DNE message
###

echo "TEST: Get deleted job should fail"

# now submit req
runurl="${APIURL}/job/${jobid}"
params=""
$SHELL $SRC_DIR/api-expect-error.sh "${runurl}" "${params}" "Job ID does not exist: ${jobid}" 404 || exit 2

echo "OK"

rm $DIR/curl.out

