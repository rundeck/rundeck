#!/bin/bash

#/ Purpose:
#/   Test the scm plugins api job status
#/ 

DIR=$(cd `dirname $0` && pwd)

source $DIR/include_scm_test.sh

ARGS=$@

JOBNAME="job status test"

setup_export_job(){
	local project=$1

	create_project $project

	do_setup_export_json_valid "export" "git-export" $project

	JOBID=$(create_job $project "$JOBNAME")
	echo $JOBID
}


test_job_status_xml(){
	local project=$1
	local integration=export

	local JOBID=$(setup_export_job $project)
	
	sleep 2

	#get job status

	METHOD=GET
	ACCEPT=application/xml
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/job/$JOBID/scm/$integration/status"
	test_begin "SCM Job Status (xml)"
	api_request $ENDPOINT $DIR/curl.out

	assert_xml_value "1" 'count(/scmJobStatus/actions/string)' $DIR/curl.out
	assert_xml_value "job-commit" '/scmJobStatus/actions/string' $DIR/curl.out
	assert_xml_value "" '/scmJobStatus/commit' $DIR/curl.out
	assert_xml_value "$integration" '/scmJobStatus/integration' $DIR/curl.out
	assert_xml_value "Created" '/scmJobStatus/message' $DIR/curl.out
	assert_xml_value "$project" '/scmJobStatus/project' $DIR/curl.out
	assert_xml_value "CREATE_NEEDED" '/scmJobStatus/synchState' $DIR/curl.out

	test_succeed

	remove_project $project
}


test_job_status_json(){
	local project=$1
	local integration=export

	local JOBID=$(setup_export_job $project)
	
	sleep 2

	#get job status

	METHOD=GET
	ACCEPT=application/json
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/job/$JOBID/scm/$integration/status"
	test_begin "SCM Job Status (json)"
	api_request $ENDPOINT $DIR/curl.out

	assert_json_value "1" '.actions | length' $DIR/curl.out
	assert_json_value "job-commit" '.actions[0]' $DIR/curl.out
	assert_json_null '.commit' $DIR/curl.out
	assert_json_value "$integration" '.integration' $DIR/curl.out
	assert_json_value "Created" '.message' $DIR/curl.out
	assert_json_value "$project" '.project' $DIR/curl.out
	assert_json_value "CREATE_NEEDED" '.synchState' $DIR/curl.out

	test_succeed

	remove_project $project
}

main(){
	test_job_status_xml "testscm-job-status-1"
	test_job_status_json "testscm-job-status-2"
}

main