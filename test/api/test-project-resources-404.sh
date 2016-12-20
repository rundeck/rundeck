#!/bin/bash

#test get resources for non-existent project

DIR=$(cd `dirname $0` && pwd)
export API_VERSION=2 #/api/2/project/NAME/resources
source $DIR/include.sh

file=$DIR/curl.out

# now submit req
proj="DNEPROJECT"


# get listing
ENDPOINT="${APIURL}/project/${proj}/resources"

test_begin "missing project results in 404"

EXPECT_STATUS=404 \
H_ACCEPT=application/xml \
PARAMS="" \
  api_request $ENDPOINT $DIR/curl.out || fail "ERROR: failed request"

test_succeed

