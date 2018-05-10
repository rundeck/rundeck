#!/bin/bash

#test listing project sources

DIR=$(cd `dirname $0` && pwd)
export API_CURRENT_VERSION=${API_VERSION}
export API_VERSION=23 #/api/23/project/NAME/sources
source $DIR/include.sh

file=$DIR/curl.out

proj="test"

ENDPOINT="${APIURL}/project/${proj}/sources"
ACCEPT="application/json"

test_begin "List project sources (json)"

api_request $ENDPOINT ${file}

assert_json_value '1' 'length'  ${file}
assert_json_value 'file'  '.[0].type' ${file}
assert_json_value '1'  '.[0].index' ${file}
assert_json_value 'false'  '.[0].resources.writeable' ${file}
assert_json_value "${RDURL}/api/${API_CURRENT_VERSION}/project/${proj}/source/1/resources" '.[0].resources.href' ${file}

test_succeed

rm ${file}
