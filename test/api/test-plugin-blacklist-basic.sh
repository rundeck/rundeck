#!/bin/bash

#test /api/projects

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

# now submit req
runurl="${APIURL}/plugins/list"

echo "TEST: /api/projects "

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

assert_json_value '74' 'length' $DIR/curl.out


rm $DIR/curl.out