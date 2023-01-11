#!/bin/bash

#test /api/projects

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

# now submit req
runurl="${APIURL}/plugin/list"

echo "TEST: /api/projects "

# get listing
docurl ${runurl}?${params} > $DIR/curl.out

echo $DIR/curl.out

test_plugin_present 'cyberark' $DIR/curl.out
test_plugin_present 'openssh' $DIR/curl.out
test_plugin_present 'ansible' $DIR/curl.out

rm $DIR/curl.out