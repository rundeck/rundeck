#!/bin/bash


#Usage:
#   util-job-delete.sh [URL] [id]
#   Deletes specified job ID.
#

errorMsg() {
   echo "$*" 1>&2
}

DIR=$(cd `dirname $0` && pwd)

# accept url argument on commandline, if '-' use default
url="$1"
if [ "-" == "$1" ] ; then
    url='http://localhost:4440'
fi
apiurl="${url}/api"

VERSHEADER="X-RUNDECK-API-VERSION: 1.2"

# curl opts to use a cookie jar, and follow redirects, showing only errors
CURLOPTS="-s -S -L -c $DIR/cookies -b $DIR/cookies"
CURL="curl $CURLOPTS"

if [ ! -f $DIR/cookies ] ; then 
    # call rundecklogin.sh
    sh $DIR/rundecklogin.sh $url
fi

XMLSTARLET=xml

###
# DELETE the chosen id, expect success message
###

jobid=$2

echo "Deleting job ID ${jobid}..."


# now submit req
runurl="${apiurl}/job/${jobid}"
params=""

# get listing
$CURL --header "$VERSHEADER" -X DELETE ${runurl}?${params} > $DIR/curl.out
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

$XMLSTARLET sel -T -t -v "/result/success/message" -n  $DIR/curl.out
    
rm $DIR/curl.out

