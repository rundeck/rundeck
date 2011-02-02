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

echo "TEST: import RunDeck Jobs in jobs.xml format"

#specify incorrect format
params="format=DNEformat"

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

#expect unsupported format error message
waserror=$($XMLSTARLET sel -T -t -v "/result/@error" $DIR/curl.out)
errmsg=$($XMLSTARLET sel -T -t -v "/result/error/message" $DIR/curl.out)
if [ "true" == "$waserror" -a "The specified format is not supported: DNEformat" == "$errmsg" ] ; then
    echo "OK"
else
    errorMsg "TEST FAILED: unsupported format message expected: $errmsg"
    exit 2
fi

##
# try to make GET request without import file content
##

# import
$CURL -D $DIR/headers.out --header "$VERSHEADER" ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

grep 'HTTP/1.1 405 Method Not Allowed' -q $DIR/headers.out
if [ 0 != $? ] ; then
    errorMsg "FAIL: Expected 405 HTTP response"
    exit 2
fi

##
# try to make POST request without import file content
##

# import
$CURL -D $DIR/headers.out -F x=y --header "$VERSHEADER" ${runurl}?${params} > $DIR/curl.out
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

#expect unsupported format error message
waserror=$($XMLSTARLET sel -T -t -v "/result/@error" $DIR/curl.out)
errmsg=$($XMLSTARLET sel -T -t -v "/result/error/message" $DIR/curl.out)
if [ "true" == "$waserror" -a "No file was uploaded" == "$errmsg" ] ; then
    echo "OK"
else
    errorMsg "TEST FAILED: no file included message expected: $errmsg"
    exit 2
fi


rm $DIR/curl.out
rm $DIR/temp.out
rm $DIR/headers.out

