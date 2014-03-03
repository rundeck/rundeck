#!/bin/bash

#test /api/projects
#using API v11, no xml result wrapper

# use api V11
API_VERSION=11
API_XML_NO_WRAPPER=true

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

# now submit req
runurl="${APIURL}/projects"

echo "TEST: /api/11/projects "

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

API_XML_NO_WRAPPER=true sh $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/projects/@count" $DIR/curl.out)
if [ "" == "$itemcount" ] ; then
    errorMsg "Wrong count"
    exit 2
    
fi

echo "OK"



rm $DIR/curl.out

