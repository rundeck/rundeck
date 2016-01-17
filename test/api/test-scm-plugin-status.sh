#!/bin/bash

#/ Purpose:
#/   Test the scm plugins api status
#/ 

DIR=$(cd `dirname $0` && pwd)

source $DIR/include_scm_test.sh

test_plugin_status_xml(){
	local integration=$1
	local plugin=$2
	local project=$3

	ENDPOINT="${APIURL}/project/$project/scm/$integration/status"
	
	test_begin "Setup SCM status: XML"

	create_project $project
	
	do_setup_export_json_valid $integration $plugin $project
	
	METHOD=GET
	ACCEPT=application/xml
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/project/$project/scm/$integration/status"
	api_request $ENDPOINT $DIR/curl.out

	assert_xml_value "$integration" '/scmProjectStatus/integration' $DIR/curl.out
	assert_xml_value "$project" '/scmProjectStatus/project' $DIR/curl.out
	assert_xml_value "CLEAN" '/scmProjectStatus/synchState' $DIR/curl.out
	assert_xml_value "" '/scmProjectStatus/message' $DIR/curl.out
	assert_xml_value "" '/scmProjectStatus/actions' $DIR/curl.out
	
	test_succeed

	remove_project $project
}

test_plugin_status_json(){
	local integration=$1
	local plugin=$2
	local project=$3

	ENDPOINT="${APIURL}/project/$project/scm/$integration/status"
	
	test_begin "Setup SCM status: JSON"

	create_project $project
	
	do_setup_export_json_valid $integration $plugin $project
	
	METHOD=GET
	ACCEPT=application/json
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/project/$project/scm/$integration/status"
	api_request $ENDPOINT $DIR/curl.out

	assert_json_value "$integration" '.integration' $DIR/curl.out
	assert_json_value "$project" '.project' $DIR/curl.out

	assert_json_value "CLEAN" '.synchState' $DIR/curl.out
	assert_json_null '.message' $DIR/curl.out
	assert_json_null '.actions' $DIR/curl.out

	test_succeed

	remove_project $project

}


main(){
	test_plugin_status_xml "export" "git-export" "testscm-plugin-status-1"
	test_plugin_status_json "export" "git-export" "testscm-plugin-status-2"

}

main