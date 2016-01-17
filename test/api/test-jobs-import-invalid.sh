#!/bin/bash

#test /api/jobs/import with invalid input

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

echo "TEST: /jobs/import with invalid format"

#specify incorrect format
params="format=DNEformat"

# specify the file for upload with curl, named "xmlBatch"
ulopts="-F xmlBatch=@$DIR/temp.out"

CURL_REQ_OPTS=$ulopts $SHELL $SRC_DIR/api-expect-error.sh "${runurl}" "${params}" "The specified format is not supported: DNEformat" || exit 2
echo "OK"

##
# try to make GET request without import file content
##
echo "TEST: /jobs/import with wrong http Method"

$SHELL $SRC_DIR/api-expect-code.sh 405 "${runurl}" "${params}" || exit 2
echo "OK"


##
# try to make POST request without import file content
##

echo "TEST: /jobs/import without expected file content"
params="xmlBatch=z"

CURL_REQ_OPTS="-F x=y" $SHELL $SRC_DIR/api-expect-error.sh "${runurl}" "${params}" "No file was uploaded" || exit 2
echo "OK"

##
# try to make POST request without expected xmlBatch parameter (multipart)
##

echo "TEST: /jobs/import multipart without xmlBatch param"
params=""

CURL_REQ_OPTS="-F x=y" $SHELL $SRC_DIR/api-expect-error.sh "${runurl}" "${params}" "parameter \"xmlBatch\" is required" || exit 2
echo "OK"

##
# try to make POST request without expected xmlBatch parameter (form)
##

echo "TEST: /jobs/import form without xmlBatch param"
params=""

CURL_REQ_OPTS="--data-urlencode x=y" $SHELL $SRC_DIR/api-expect-error.sh "${runurl}" "${params}" "parameter \"xmlBatch\" is required" || exit 2
echo "OK"

##
# specify invalid import content
##

#No context/project value
cat > $DIR/temp.out <<END
<joblist>
   <job>
      <name>cli job</name>
      <group>api-test</group>
      <description></description>
      <loglevel>INFO</loglevel>
      <context>
          <project>DNEProj</project>
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

APIv13URL="${RDURL}/api/13"
runurl="${APIv13URL}/jobs/import"
params="format=xml"
# specify the file for upload with curl, named "xmlBatch"
ulopts="-F xmlBatch=@$DIR/temp.out"

echo "TEST: /jobs/import with bad definition (api < v14)"

# get listing
docurl $ulopts  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

# expect a /result/failed item

failedcount=$($XMLSTARLET sel -T -t -v "/result/failed/@count" $DIR/curl.out)
succount=$($XMLSTARLET sel -T -t -v "/result/succeeded/@count" $DIR/curl.out)
skipcount=$($XMLSTARLET sel -T -t -v "/result/skipped/@count" $DIR/curl.out)

if [ "1" != "$failedcount" ] ; then
    errorMsg  "Upload was not successful."
    exit 2
fi

# verify results
jid=$($XMLSTARLET sel -T -t -v "/result/failed/job/id" $DIR/curl.out)
jname=$($XMLSTARLET sel -T -t -v "/result/failed/job/name" $DIR/curl.out)
jgroup=$($XMLSTARLET sel -T -t -v "/result/failed/job/group" $DIR/curl.out)
jproj=$($XMLSTARLET sel -T -t -v "/result/failed/job/project" $DIR/curl.out)

assert "cli job" "$jname" "Wrong job name: $jname"
assert "api-test" "$jgroup" "Wrong job group: $jgroup"
assert "DNEProj" "$jproj" "Wrong job project: $jproj"

if [ "" != "$jid" ] ; then
    errorMsg "Did not expect job id in result: $jid"
    exit 2
fi

echo "OK"



rm $DIR/curl.out
rm $DIR/temp.out

