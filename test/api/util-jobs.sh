#!/bin/bash

#Usage: 
#    util-jobs.sh <URL> <project> [param=value&...]

if [ $# -lt 2 ] ; then
    echo "Usage: util-jobs.sh <URL> <project> [param=value&..]"
    exit 2
fi

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

proj=$1
shift

# now submit req
runurl="${APIURL}/jobs"

echo "# Listing RunDeck Jobs for project ${proj}..."

args=$@
params="project=${proj}&$args"


# get listing
docurl ${runurl}?${params} > $DIR/curl.out|| fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$(xmlsel "/result/jobs/@count" $DIR/curl.out)
#echo "$itemcount Jobs"    
if [ "0" != "$itemcount" ] ; then
    #echo all on one line
    $XMLSTARLET sel -T -t -m "/result/jobs/job" -v "@id" -o " " -v "name" -o " (" -v "group" -o ") " -o ": &quot;" -v "description" -o '&quot;'  -n $DIR/curl.out
fi

rm $DIR/curl.out

