#!/bin/bash

#test output from /api/job/{id}

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
      <group>api-test/job-export</group>
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

# expect success result
sh $DIR/api-test-success.sh $DIR/curl.out || exit 2


#result will contain list of failed and succeeded jobs, in this
#case there should only be 1 failed or 1 succeeded since we submit only 1

succount=$($XMLSTARLET sel -T -t -v "/result/succeeded/@count" $DIR/curl.out)
jobid=$($XMLSTARLET sel -T -t -v "/result/succeeded/job/id" $DIR/curl.out)

if [ "1" != "$succount" -o "" == "$jobid" ] ; then
    errorMsg  "Upload was not successful."
    exit 
fi

###
# Export the chosen id
###

echo "TEST: export single job in jobs.xml format"


# now submit req
runurl="${apiurl}/job/${jobid}"
params=""

# get listing
$CURL --header "$VERSHEADER" ${runurl}?${params} > $DIR/curl.out
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
$XMLSTARLET el $DIR/curl.out | grep -e '^joblist' -q
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response did not contain expected result"
    exit 2
fi

# job export doesn't wrap result in common result wrapper
#Check projects list
itemcount=$($XMLSTARLET sel -T -t -m "/joblist" -v "count(job)" $DIR/curl.out)
foundjobid=$($XMLSTARLET sel -T -t -m "/joblist" -v "job/id" $DIR/curl.out)
if [ "1" == "$itemcount" -a "$jobid" == "$foundjobid" ] ; then
    echo "OK"
else
    errMsg "Wrong job count: $itemcount, or wrong found id: $foundjobid"
    exit 2
fi


###
# Export the chosen id, with format=yaml
###

echo "TEST: export single job in jobs.yaml format"


# now submit req
runurl="${apiurl}/job/${jobid}"
params="format=yaml"

# get listing
$CURL -D $DIR/headers.out --header "$VERSHEADER" ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#test headers.out for valid yaml content type
grep "Content-Type: text/yaml" $DIR/headers.out -q
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response was not yaml"
    exit 2
fi

#test yaml output for at least the id: entry
grep "id: ${jobid}" $DIR/curl.out -q
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response yaml did not have expected job id"
    exit 2
fi
echo OK

rm $DIR/curl.out

