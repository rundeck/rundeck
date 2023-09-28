#!/bin/bash

#test /api/projects
#using API v14, no xml result wrapper

# use api V14
API_VERSION=14
API_XML_NO_WRAPPER=true

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

# now submit req
runurl="${APIURL}/projects"

echo "TEST: /api/14/projects "

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

API_XML_NO_WRAPPER=true $SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2


assert_xml_notblank "/projects/@count" $DIR/curl.out

echo "OK"



rm $DIR/curl.out

