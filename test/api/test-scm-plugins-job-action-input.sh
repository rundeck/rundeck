#!/bin/bash

#/ Purpose:
#/   Test the scm plugins api job status
#/ 

DIR=$(cd `dirname $0` && pwd)

source $DIR/include_scm_test.sh

JOBNAME="job actions test"

setup_export_job(){
	local project=$1

	create_project $project

	do_setup_export_json_valid "export" "git-export" $project

	JOBID=$(create_job $project "$JOBNAME")
	echo $JOBID
}


test_job_action_input_xml(){
	local project=$1
	local integration=export

	local JOBID=$(setup_export_job $project)
	local actionId="job-commit"
	
	sleep 2

	#get job status

	METHOD=GET
	ACCEPT=application/xml
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/job/$JOBID/scm/$integration/action/$actionId/input"
	test_begin "SCM Job Action Input (xml)"
	api_request $ENDPOINT $DIR/curl.out

	# test "message" field
	assert_xml_value "$actionId" '/scmActionInput/actionId' $DIR/curl.out
	assert_xml_value "export" '/scmActionInput/integration' $DIR/curl.out
	assert_xml_value "Commit Changes to Git" '/scmActionInput/title' $DIR/curl.out
	assert_xml_value "3" 'count(/scmActionInput/fields/scmPluginInputField)' $DIR/curl.out
	assert_xml_value "Commit Message" '/scmActionInput/fields/scmPluginInputField[name="message"]/title' $DIR/curl.out
	assert_xml_value "Tag" '/scmActionInput/fields/scmPluginInputField[name="tagName"]/title' $DIR/curl.out
	assert_xml_value "Push Remotely?" '/scmActionInput/fields/scmPluginInputField[name="push"]/title' $DIR/curl.out

	# test export item for the job
	assert_xml_value "1" 'count(/scmActionInput/exportItems/scmExportActionItem)' $DIR/curl.out
	assert_xml_value "$JOBID" '/scmActionInput/exportItems/scmExportActionItem/job/jobId' $DIR/curl.out
	assert_xml_value "$JOBNAME" '/scmActionInput/exportItems/scmExportActionItem/job/jobName' $DIR/curl.out
	assert_xml_value "" '/scmActionInput/exportItems/scmExportActionItem/job/groupPath' $DIR/curl.out
	assert_xml_value "false" '/scmActionInput/exportItems/scmExportActionItem/deleted' $DIR/curl.out
	assert_xml_value "false" '/scmActionInput/exportItems/scmExportActionItem/renamed' $DIR/curl.out
	assert_xml_value "$JOBNAME-$JOBID.xml" '/scmActionInput/exportItems/scmExportActionItem/itemId' $DIR/curl.out
	assert_xml_value "" '/scmActionInput/exportItems/scmExportActionItem/originalId' $DIR/curl.out

	test_succeed

	remove_project $project
}


test_job_action_input_json(){
	local project=$1
	local integration=export

	local JOBID=$(setup_export_job $project)
	local actionId="job-commit"
	
	sleep 2

	#get job status

	METHOD=GET
	ACCEPT=application/json
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/job/$JOBID/scm/$integration/action/$actionId/input"
	test_begin "SCM Job Action Input (json)"
	api_request $ENDPOINT $DIR/curl.out

	# test "message" field
	assert_json_value "$actionId" '.actionId' $DIR/curl.out
	assert_json_value "export" '.integration' $DIR/curl.out
	assert_json_value "Commit Changes to Git" '.title' $DIR/curl.out
	assert_json_value "3" '.fields | length' $DIR/curl.out
	assert_json_value "Commit Message" '.fields[] | select(.name == "message") | .title' $DIR/curl.out
	assert_json_value "Tag" '.fields[] | select(.name == "tagName") | .title' $DIR/curl.out
	assert_json_value "Push Remotely?" '.fields[] | select(.name == "push") | .title' $DIR/curl.out

	# test export item for the job
	assert_json_value "1" '.exportItems | length' $DIR/curl.out
	assert_json_value "$JOBID" '.exportItems[0].job.jobId' $DIR/curl.out
	assert_json_value "$JOBNAME" '.exportItems[0].job.jobName' $DIR/curl.out
	assert_json_null '.exportItems[0].job.groupPath' $DIR/curl.out
	assert_json_value "false" '.exportItems[0].deleted' $DIR/curl.out
	assert_json_value "false" '.exportItems[0].renamed' $DIR/curl.out
	assert_json_value "$JOBNAME-$JOBID.xml" '.exportItems[0].itemId' $DIR/curl.out
	assert_json_null '.exportItems[0].originalId' $DIR/curl.out

	test_succeed

	remove_project $project
}

main(){
	test_job_action_input_xml "testscm1"
	test_job_action_input_json "testscm2"
}

main