#!/bin/bash

#test /api/projects

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

# now submit req
runurl="${APIURL}/plugin/list"

echo "TEST: /api/projects "

# get listing
docurl ${runurl}?${params} > $DIR/curl.out

assert_json_value '58' 'length' $DIR/curl.out


rm $DIR/curl.out