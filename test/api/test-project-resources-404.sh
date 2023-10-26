#!/bin/bash

#test get resources for non-existent project

DIR=$(cd `dirname $0` && pwd)
export API_VERSION=14
source $DIR/include.sh

file=$DIR/curl.out

# now submit req
proj="DNEPROJECT"
APIURL="${RDURL}/api/${API_VERSION}"


# get listing
ENDPOINT="${APIURL}/project/${proj}/resources"

test_begin "missing project results in 404"

EXPECT_STATUS=404 \
H_ACCEPT=application/json \
PARAMS="" \
  api_request $ENDPOINT $DIR/curl.out || fail "ERROR: failed request"

test_succeed

