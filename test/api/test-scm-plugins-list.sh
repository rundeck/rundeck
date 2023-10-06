#!/bin/bash

#/ Purpose:
#/   Test the scm plugins list endpoints
#/ 

DIR=$(cd `dirname $0` && pwd)
export API_XML_NO_WRAPPER=1
source $DIR/include_scm_test.sh

project="testscmlist"

test_plugins_list_json(){
	local intname=$1
	ENDPOINT="${APIURL}/project/$project/scm/$intname/plugins"

	test_begin "JSON response"

	ACCEPT=application/json

	api_request $ENDPOINT $DIR/curl.out
	

	#Check projects list
	assert_json_value $intname '.integration' $DIR/curl.out
	assert_json_value '1' '.plugins | length' $DIR/curl.out
	assert_json_value "git-$intname" '.plugins[0].type' $DIR/curl.out
	assert_json_value "false" '.plugins[0].configured' $DIR/curl.out
	assert_json_value "false" '.plugins[0].enabled' $DIR/curl.out

	test_succeed

}
test_plugins_list_json_failure(){
	local intname=$1
	ENDPOINT="${APIURL}/project/$project/scm/$intname/plugins"

	test_begin "JSON response/Invalid integration"

	ACCEPT=application/json
	EXPECT_STATUS=400

	api_request $ENDPOINT $DIR/curl.out

	#Check projects list
	assert_json_value 'true' '.error' $DIR/curl.out
	assert_json_value "Invalid API Request: the value \"$intname\" for parameter \"integration\" was invalid. It must be in the list: [export, import]" \
	 '.message' $DIR/curl.out

	test_succeed

}

main(){
	create_project $project
	test_plugins_list_json 'import'
	test_plugins_list_json 'export'
	test_plugins_list_json_failure 'invalid'
	remove_project $project
}

main