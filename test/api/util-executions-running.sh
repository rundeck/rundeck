#!/bin/bash

#Usage: 
#    util-executions-running.sh <URL> <project>

DIR=$(cd `dirname $0` && pwd)

source $DIR/include.sh

proj=$1
if [ "" == "$1" ] ; then
    proj="test"
fi
shift

# now submit req
runurl="${APIURL}/executions/running"

echo "# Listing Running Executions for project ${proj}..."

params="project=${proj}"


# get listing
docurl  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$(xmlsel "/result/executions/@count" $DIR/curl.out)
#echo "$itemcount Jobs"    
if [ "0" != "$itemcount" ] ; then
    #echo all on one line
    $XMLSTARLET sel -T -t -m "/result/executions/execution" -o "[" -v "@id" -o "] " -v "description" -o ": " -v "job/name" -n $DIR/curl.out
fi

rm $DIR/curl.out

