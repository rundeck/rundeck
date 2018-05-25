#!/bin/bash

#test listing project sources

DIR=$(cd `dirname $0` && pwd)
export API_CURRENT_VERSION=${API_VERSION}
export API_VERSION=23 #/api/23/project/NAME/sources
source $DIR/include.sh

file=$DIR/curl.out

proj="test"

ENDPOINT="${APIURL}/project/${proj}/sources"
ACCEPT="application/xml"

test_begin "List project sources (xml)"

api_request $ENDPOINT ${file}

assert_xml_valid ${file}

assert_xml_value 'test' '//sources/@project' ${file}
assert_xml_value '1' '//sources/@count'  ${file}
assert_xml_value '1' 'count(/sources/source)'  ${file}
assert_xml_value 'file'  '/sources/source/@type' ${file}
assert_xml_value '1'  '/sources/source/@index' ${file}
assert_xml_value 'false'  '/sources/source/resources/@writeable' ${file}
assert_xml_value "${RDURL}/api/${API_CURRENT_VERSION}/project/${proj}/source/1/resources" '/sources/source/resources/@href' ${file}

test_succeed

rm ${file}
