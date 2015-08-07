#!/bin/bash

#Usage:
#    util-history.sh <URL> <project> [param=value [param=value] .. ]

DIR=$(cd `dirname $0` && pwd)

source $DIR/include.sh

proj=$1
if [ "" == "$1" ] ; then
    proj="test"
fi
shift

# now submit req
runurl="${APIURL}/history"

echo "# Listing RunDeck Jobs for project ${proj}..."

args="$*"
params="project=${proj}&${args}"


# get listing
docurl  ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$(xmlsel "/result/events/@count" $DIR/curl.out)

if [ "0" != "$itemcount" ] ; then
    #echo all on one line
    $XMLSTARLET sel -T -t -m "/result/events/event" -o "[" \
        -v "date-ended" -o "] " \
        -v "user" -o " : " -v "project" -o " [" \
        -v "job/@id" -o "," -v "execution/@id"  -o "] [" \
        -v "node-summary/@succeeded" -o "/" \
        -v "node-summary/@failed" -o "/" \
        -v "node-summary/@total" -o "] " \
        -v "title" -o " : " \
        -v "normalize-space(summary)"  \
        -n $DIR/curl.out
fi

#rm $DIR/curl.out
