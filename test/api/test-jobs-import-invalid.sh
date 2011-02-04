#!/bin/bash

#test /api/jobs/import with invalid input

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
runurl="${apiurl}/jobs/import"

echo "TEST: /jobs/import with invalid format"

#specify incorrect format
params="format=DNEformat"

# specify the file for upload with curl, named "xmlBatch"
ulopts="-F xmlBatch=@$DIR/temp.out"

CURL_REQ_OPTS=$ulopts sh $DIR/api-expect-error.sh "${runurl}" "${params}" "The specified format is not supported: DNEformat" || exit 2
echo "OK"

##
# try to make GET request without import file content
##
echo "TEST: /jobs/import with wrong http Method"

sh $DIR/api-expect-code.sh 405 "${runurl}" "${params}" || exit 2
echo "OK"


##
# try to make POST request without import file content
##

echo "TEST: /jobs/import without expected file content"

CURL_REQ_OPTS="-F x=y" sh $DIR/api-expect-error.sh "${runurl}" "${params}" "No file was uploaded" || exit 2
echo "OK"

rm $DIR/curl.out
rm $DIR/temp.out

