#!/bin/bash

# Usage: util-jobs-import.sh <url> <file> [format]

errorMsg() {
   echo "$*" 1>&2
}

DIR=$(cd `dirname $0` && pwd)

# accept url argument on commandline, if '-' use default
url="$1"
if [ "-" == "$1" ] ; then
    url='http://localhost:4440/api'
fi
shift
apiurl="${url}/api"

VERSHEADER="X-RUNDECK-API-VERSION: 1.2"

# curl opts to use a cookie jar, and follow redirects, showing only errors
CURLOPTS="-s -S -L -c $DIR/cookies -b $DIR/cookies"
CURL="curl $CURLOPTS"

XMLSTARLET=xml

infile=$1
shift
informat=${1:-xml}


# now submit req
runurl="${apiurl}/jobs/import"

echo "Import jobs in ${informat} format"

params="dupeOption=update"

# specify the file for upload with curl, named "xmlBatch"
ulopts="-F xmlBatch=@$infile"

# get listing
$CURL $ulopts --header "$VERSHEADER" ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

sh $DIR/api-test-success.sh $DIR/curl.out || exit 2

#result will contain list of failed and succeeded jobs, in this
#case there should only be 1 failed or 1 succeeded since we submit only 1

failedcount=$($XMLSTARLET sel -T -t -v "/result/failed/@count" $DIR/curl.out)
succount=$($XMLSTARLET sel -T -t -v "/result/succeeded/@count" $DIR/curl.out)
skipcount=$($XMLSTARLET sel -T -t -v "/result/skipped/@count" $DIR/curl.out)

if [ "0" != "$failedcount" ] ; then
    echo "$failedcount Failed:"
    $XMLSTARLET sel -T -t -m "/result/failed/job" -o "[" -v "id" -o "] " -v "name" -o ", " -v "group" -o ", " -v "project" -n \
    -o "ERROR: " -v "error" -n $DIR/curl.out
fi

if [ "0" != "$succount" ] ; then
    echo "$failedcount Succeeded:"
    $XMLSTARLET sel -T -t -m "/result/succeeded/job" -o "[" -v "id" -o "] " -v "name" -o ", " -v "group" -o ", " -v "project" -n $DIR/curl.out
fi

if [ "0" != "$skipcount" ] ; then
    echo "$failedcount Skipped:"
    $XMLSTARLET sel -T -t -m "/result/skipped/job" -o "[" -v "id" -o "] " -v "name" -o ", " -v "group" -o ", " -v "project" -n $DIR/curl.out
fi
rm $DIR/curl.out

