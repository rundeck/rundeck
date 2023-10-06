#!/bin/bash

#test output from api/10/project/{proj}
# test 400 response - bad version

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh


proj="test"
runurl="$RDURL/api/10/project/${proj}" # test v5


test_bad_version_json(){

	####
	# Test: json request with 400 response, unsupported version
	####
	ENDPOINT="$runurl"

	ACCEPT=application/json
	METHOD=GET
	EXPECT_STATUS=400

	api_request $ENDPOINT $DIR/curl.out

	assert_json_value "api.error.api-version.unsupported" ".errorCode" $DIR/curl.out

	test_succeed

}



main(){
	test_bad_version_json
}
main