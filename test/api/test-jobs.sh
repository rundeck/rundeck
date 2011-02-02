#!/bin/bash

#test output from /api/jobs

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

# now submit req
runurl="${apiurl}/jobs"
proj=$2
if [ "" == "$2" ] ; then
    proj="test"
fi

echo "Listing RunDeck Jobs for project ${proj}..."

params="project=${proj}"

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

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/jobs/@count" $DIR/curl.out)
echo "$itemcount Jobs"    
if [ "0" != "$itemcount" ] ; then
    #echo all on one line
    $XMLSTARLET sel -T -t -m "/result/jobs/job" -o "[" -v "@id" -o "] " -v "name" -o " (" -v "group" -o ") " -o ": &quot;" -v "description" -o '&quot;'  -n $DIR/curl.out
fi
echo "OK"




rm $DIR/curl.out

