#!/bin/bash

#Usage: 
#    util-jobs.sh <URL> <project> [param=value [param=value] .. ]

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

proj=$1
if [ "" == "$1" ] ; then
    proj="test"
fi
shift

# now submit req
runurl="${APIURL}/jobs"

echo "# Listing RunDeck Jobs for project ${proj}..."

params="project=${proj}"


# get listing
$CURL ${runurl}?${params} > $DIR/curl.out|| fail "failed request: ${runurl}"

sh $DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$(xmlsel "/result/jobs/@count" $DIR/curl.out)
#echo "$itemcount Jobs"    
if [ "0" != "$itemcount" ] ; then
    #echo all on one line
    $XMLSTARLET sel -T -t -m "/result/jobs/job" -v "@id" -o " " -v "name" -o " (" -v "group" -o ") " -o ": &quot;" -v "description" -o '&quot;'  -n $DIR/curl.out
fi

rm $DIR/curl.out

