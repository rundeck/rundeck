#!/bin/bash

#test output from /api/execution/{id}/output
# test 404 response

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

execid="000"
TITLE="execution/ID/output not found"

test_execution_notfound_xml(){

	####
	# Test: request with 404 response
	####


	ENDPOINT="$APIURL/execution/$execid/output"
	ACCEPT=application/xml
	EXPECT_STATUS=404

	test_begin "$TITLE (xml)"

	api_request $ENDPOINT $DIR/curl.out

	assert_xml_valid $DIR/curl.out

	test_succeed

}

test_execution_notfound_json(){

	ENDPOINT="$APIURL/execution/$execid/output"
	ACCEPT=application/json
	EXPECT_STATUS=404

	test_begin "$TITLE (json)"

	api_request $ENDPOINT $DIR/curl.out

	assert_json_value "api.error.item.doesnotexist" ".errorCode" $DIR/curl.out

	test_succeed
}

main(){
	test_execution_notfound_xml
	test_execution_notfound_json
}
main