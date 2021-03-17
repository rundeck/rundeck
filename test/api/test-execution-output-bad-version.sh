#!/bin/bash

#test output from /api/execution/{id}/output
# test 404 response

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

execid=$(xmlsel "//execution/@id" $DIR/curl.out)
TITLE="execution/ID/output not found"
runurl="$RDURL/api/5" # test v5


test_bad_version_json(){

	####
	# Test: json request with 400 response, unsupported version
	####
	ENDPOINT="$runurl/execution/$execid/output"

	ACCEPT=application/json
	METHOD=GET
	EXPECT_STATUS=400

	api_request $ENDPOINT $DIR/curl.out

	assert_json_value "api.error.api-version.unsupported" ".errorCode" $DIR/curl.out

	test_succeed

}

test_bad_version_xml(){

	####
	# Test: xml request with 400 response, unsupported version
	####
	ENDPOINT="$runurl/execution/$execid/output"
	ACCEPT=application/xml
	METHOD=GET
	EXPECT_STATUS=400

	api_request $ENDPOINT $DIR/curl.out

	assert_xml_valid $DIR/curl.out

	test_succeed

}



main(){
	test_bad_version_json
	test_bad_version_xml
}
main
