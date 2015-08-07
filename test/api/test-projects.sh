#!/bin/bash

#test /api/projects

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

# now submit req
runurl="${APIURL}/projects"

echo "TEST: /api/projects "

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/projects/@count" $DIR/curl.out)
if [ "" == "$itemcount" ] ; then
    errorMsg "Wrong count"
    exit 2
    
fi

echo "OK"



rm $DIR/curl.out

