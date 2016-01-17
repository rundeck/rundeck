#!/bin/bash

#/ Purpose:
#/   Test the scm plugins api status
#/ 

DIR=$(cd `dirname $0` && pwd)

source $DIR/include_scm_test.sh

ARGS=$@
JOBNAME="test job"

setup_export_actions_fields(){
	local project=$1

	create_project $project

	do_setup_export_json_valid "export" "git-export" $project

	JOBID=$(create_job $project "$JOBNAME")
	echo $JOBID
}
test_export_actions_fields_xml(){
	local project=$1
	local integration=export

	local JOBID=$(setup_export_actions_fields $project)
	
	sleep 2

	#list actions for status

	METHOD=GET
	ACCEPT=application/xml
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/project/$project/scm/$integration/status"
	api_request $ENDPOINT $DIR/curl.out

	assert_xml_value "1" 'count(/scmProjectStatus/actions/string)' $DIR/curl.out
	assert_xml_value "project-commit" '/scmProjectStatus/actions/string' $DIR/curl.out

	# list fields for action

	METHOD=GET
	ACCEPT=application/xml
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/project/$project/scm/$integration/action/project-commit/input"
	test_begin "GET SCM Action Input Fields (XML)"
	api_request $ENDPOINT $DIR/curl.out

	$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

	# test "message" field
	assert_xml_value "project-commit" '/scmActionInput/actionId' $DIR/curl.out
	assert_xml_value "export" '/scmActionInput/integration' $DIR/curl.out
	assert_xml_value "Commit Changes to Git" '/scmActionInput/title' $DIR/curl.out
	assert_xml_value "3" 'count(/scmActionInput/fields/scmPluginInputField)' $DIR/curl.out
	assert_xml_value "Commit Message" '/scmActionInput/fields/scmPluginInputField[name="message"]/title' $DIR/curl.out

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
test_export_perform_action_xml(){

	local project=$1
	local integration=export

	local JOBID=$(setup_export_actions_fields $project)
	
	sleep 2

	#list actions for status

	METHOD=GET
	ACCEPT=application/xml
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/project/$project/scm/$integration/status"
	api_request $ENDPOINT $DIR/curl.out

	assert_xml_value "1" 'count(/scmProjectStatus/actions/string)' $DIR/curl.out
	assert_xml_value "project-commit" '/scmProjectStatus/actions/string' $DIR/curl.out

	# get status input
	# list fields for action

	METHOD=GET
	ACCEPT=application/xml
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/project/$project/scm/$integration/action/project-commit/input"
	
	api_request $ENDPOINT $DIR/curl.out

	$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

	assert_xml_value "Commit Message" '/scmActionInput/fields/scmPluginInputField[name="message"]/title' $DIR/curl.out
	
	local jobId=$(xmlsel '/scmActionInput/exportItems/scmExportActionItem/job/jobId' $DIR/curl.out)
	local commitMessage="A test commit"

	# perform action

	METHOD=POST
	ACCEPT=application/xml
	TYPE=application/xml
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/project/$project/scm/$integration/action/project-commit"
	TMPDIR=`tmpdir`
	tmp=$TMPDIR/commit.xml
	cat >$tmp <<END
<scmAction>
	<input>
		<entry key="message">$commitMessage</entry>
	</input>
	<items>
	</items>
	<jobs>
		<job jobId="$jobId"/>
	</jobs>
	<deleted></deleted>
</scmAction>
END
	POSTFILE=$tmp
	test_begin "POST SCM Action (XML)"
	api_request $ENDPOINT $DIR/curl.out

	$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

	assert_xml_value "true" '/scmActionResult/success' $DIR/curl.out
	assert_xml_value "SCM export Action was Successful: project-commit" '/scmActionResult/message' $DIR/curl.out
	
	test_succeed

	remove_project $project
}
test_export_actions_fields_json(){
	local project=$1
	local integration=export

	local JOBID=$(setup_export_actions_fields $project)

	#list actions for status
	sleep 2

	METHOD=GET
	ACCEPT=application/json
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/project/$project/scm/$integration/status"
	api_request $ENDPOINT $DIR/curl.out

	assert_json_value "1" '.actions | length' $DIR/curl.out
	assert_json_value "project-commit" '.actions[0]' $DIR/curl.out

	# list fields for action

	METHOD=GET
	ACCEPT=application/json
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/project/$project/scm/$integration/action/project-commit/input"
	test_begin "GET SCM Action Input Fields (JSON)"
	api_request $ENDPOINT $DIR/curl.out

	#test fields
	assert_json_value "project-commit" '.actionId' $DIR/curl.out
	assert_json_value "export" '.integration' $DIR/curl.out
	assert_json_value "Commit Changes to Git" '.title' $DIR/curl.out
	assert_json_value "3" '.fields | length' $DIR/curl.out
	assert_json_value "Commit Message" '.fields[] | select(.name == "message") | .title' $DIR/curl.out

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

test_export_perform_action_json(){

	local project=$1
	local integration=export

	local JOBID=$(setup_export_actions_fields $project)
	
	sleep 2

	#list actions for status

	METHOD=GET
	ACCEPT=application/xml
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/project/$project/scm/$integration/status"
	api_request $ENDPOINT $DIR/curl.out

	assert_xml_value "1" 'count(/scmProjectStatus/actions/string)' $DIR/curl.out
	assert_xml_value "project-commit" '/scmProjectStatus/actions/string' $DIR/curl.out

	# get status input
	# list fields for action

	METHOD=GET
	ACCEPT=application/xml
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/project/$project/scm/$integration/action/project-commit/input"
	
	api_request $ENDPOINT $DIR/curl.out

	$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

	assert_xml_value "Commit Message" '/scmActionInput/fields/scmPluginInputField[name="message"]/title' $DIR/curl.out
	
	local jobId=$(xmlsel '/scmActionInput/exportItems/scmExportActionItem/job/jobId' $DIR/curl.out)
	local commitMessage="A test commit"

	# perform action

	METHOD=POST
	ACCEPT=application/json
	TYPE=application/json
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/project/$project/scm/$integration/action/project-commit"
	TMPDIR=`tmpdir`
	tmp=$TMPDIR/commit.json
	cat >$tmp <<END
{
	"input":{
		"message":"$commitMessage"
	},
	"jobs":[
		"$jobId"
	],
	"items":null,
	"deleted":null
}
END
	POSTFILE=$tmp
	test_begin "POST SCM Action (JSON)"
	api_request $ENDPOINT $DIR/curl.out

	assert_json_value "true" '.success' $DIR/curl.out
	assert_json_value "SCM export Action was Successful: project-commit" '.message' $DIR/curl.out
	
	test_succeed

	remove_project $project
}

main(){
	test_export_actions_fields_xml "testscm1"
	test_export_perform_action_xml "testscm2"

	test_export_actions_fields_json "testscm3"
	test_export_perform_action_json "testscm4"
}

main