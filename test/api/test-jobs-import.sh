#!/bin/bash

#test /api/jobs/import

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

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

echo "TEST: import RunDeck Jobs in jobs.xml format (multipart request)"

params=""

# specify the file for upload with curl, named "xmlBatch"
ulopts="-F xmlBatch=@$DIR/temp.out"

# get listing
docurl $ulopts  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi


#result will contain list of failed and succeeded jobs, in this
#case there should only be 1 failed or 1 succeeded since we submit only 1

failedcount=$(xmlsel "//failed/@count" $DIR/curl.out)
succount=$(xmlsel "//succeeded/@count" $DIR/curl.out)
skipcount=$(xmlsel "//skipped/@count" $DIR/curl.out)

if [ "1" != "$succount" ] ; then
    errorMsg  "Upload was not successful."
    exit 2
fi

# verify results
jid=$(xmlsel "//succeeded/job/id" $DIR/curl.out)
jname=$(xmlsel "//succeeded/job/name" $DIR/curl.out)
jgroup=$(xmlsel "//succeeded/job/group" $DIR/curl.out)
jproj=$(xmlsel "//succeeded/job/project" $DIR/curl.out)

assert "cli job" "$jname" "Wrong job name: $jname"
assert "api-test" "$jgroup" "Wrong job group: $jgroup"
assert "$project" "$jproj" "Wrong job project: $jproj"

if [ -z "$jid" ] ; then
    errorMsg "Expected job id in result: $jid"
    exit 2
fi

echo "OK"

# test upload via form content instead of multipart file


echo "TEST: import RunDeck Jobs in jobs.xml format (urlencode)"

params=""

# specify the file for upload with curl, named "xmlBatch"
ulopts="--data-urlencode xmlBatch@$DIR/temp.out"

# get listing
docurl $ulopts  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi


#result will contain list of failed and succeeded jobs, in this
#case there should only be 1 failed or 1 succeeded since we submit only 1

failedcount=$(xmlsel "//failed/@count" $DIR/curl.out)
succount=$(xmlsel "//succeeded/@count" $DIR/curl.out)
skipcount=$(xmlsel "//skipped/@count" $DIR/curl.out)

if [ "1" != "$succount" ] ; then
    errorMsg  "Upload was not successful."
    exit 2
fi

# verify results
jid=$(xmlsel "//succeeded/job/id" $DIR/curl.out)
jname=$(xmlsel "//succeeded/job/name" $DIR/curl.out)
jgroup=$(xmlsel "//succeeded/job/group" $DIR/curl.out)
jproj=$(xmlsel "//succeeded/job/project" $DIR/curl.out)

assert "cli job" "$jname" "Wrong job name: $jname"
assert "api-test" "$jgroup" "Wrong job group: $jgroup"
assert "$project" "$jproj" "Wrong job project: $jproj"

if [ -z "$jid" ] ; then
    errorMsg "Expected job id in result: $jid"
    exit 2
fi

echo "OK"


rm $DIR/curl.out
rm $DIR/temp.out
