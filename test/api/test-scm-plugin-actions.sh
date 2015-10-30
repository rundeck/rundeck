#!/bin/bash

#/ Purpose:
#/   Test the scm plugins api status
#/ 

DIR=$(cd `dirname $0` && pwd)

source $DIR/include_scm_test.sh

ARGS=$@
JOBNAME="test job"

create_job(){
	local project=$1


	TMPDIR=`tmpdir`
	tmp=$TMPDIR/job.xml
	cat >$tmp <<END
<joblist>
  <job>
    <description></description>
    <executionEnabled>true</executionEnabled>
    <loglevel>INFO</loglevel>
    <name>$JOBNAME</name>
    <scheduleEnabled>true</scheduleEnabled>
    <sequence keepgoing='false' strategy='node-first'>
      <command>
        <exec>echo hi</exec>
      </command>
    </sequence>
  </job>
</joblist>
END
	METHOD=POST
	ENDPOINT="${APIURL}/project/$project/jobs/import"
	ACCEPT=application/xml
	TYPE=application/xml
	POSTFILE=$tmp

	api_request $ENDPOINT $DIR/curl.out


	assert_xml_value "1" '/result/succeeded/@count' $DIR/curl.out
	local JOBID=$( xmlsel '/result/succeeded/job/id' $DIR/curl.out )

	echo $JOBID
}
setup_export_actions_fields(){
	local project=$1

	create_project $project

	do_setup_export_json_valid "export" "git-export" $project

	JOBID=$(create_job $project)
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
	assert_xml_value "project-commit" '/scmProjectActionInput/actionId' $DIR/curl.out
	assert_xml_value "export" '/scmProjectActionInput/integration' $DIR/curl.out
	assert_xml_value "Commit Changes to Git" '/scmProjectActionInput/title' $DIR/curl.out
	assert_xml_value "3" 'count(/scmProjectActionInput/fields/scmPluginInputField)' $DIR/curl.out
	assert_xml_value "Commit Message" '/scmProjectActionInput/fields/scmPluginInputField[name="message"]/title' $DIR/curl.out

	# test export item for the job
	assert_xml_value "1" 'count(/scmProjectActionInput/exportItems/scmExportActionItem)' $DIR/curl.out
	assert_xml_value "$JOBID" '/scmProjectActionInput/exportItems/scmExportActionItem/job/jobId' $DIR/curl.out
	assert_xml_value "$JOBNAME" '/scmProjectActionInput/exportItems/scmExportActionItem/job/jobName' $DIR/curl.out
	assert_xml_value "" '/scmProjectActionInput/exportItems/scmExportActionItem/job/groupPath' $DIR/curl.out
	assert_xml_value "false" '/scmProjectActionInput/exportItems/scmExportActionItem/deleted' $DIR/curl.out
	assert_xml_value "false" '/scmProjectActionInput/exportItems/scmExportActionItem/renamed' $DIR/curl.out
	assert_xml_value "$JOBNAME-$JOBID.xml" '/scmProjectActionInput/exportItems/scmExportActionItem/itemId' $DIR/curl.out
	assert_xml_value "" '/scmProjectActionInput/exportItems/scmExportActionItem/originalId' $DIR/curl.out

	test_succeed

	remove_project $project
}
test_export_perform_action_xml(){

	local project=$1
	local integration=export

	setup_export_actions_fields $project
	
	sleep 2

	#list actions for status

	METHOD=GET
	ACCEPT=application/xml
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/project/$project/scm/$integration/status"
	api_request $ENDPOINT $DIR/curl.out

	assert_xml_value "1" 'count(/scmProjectStatus/actions/string)' $DIR/curl.out
	assert_xml_value "project-commit" '/scmProjectStatus/actions/string' $DIR/curl.out

	# perform action

	METHOD=POST
	ACCEPT=application/xml
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/project/$project/scm/$integration/action/project-commit"
	TMPDIR=`tmpdir`
	tmp=$TMPDIR/job.xml
	cat >$tmp <<END
<scmActionRequest>

</scmActionRequest>
END
	POSTFILE=$tmp
	test_begin "POST SCM Action (XML)"
	api_request $ENDPOINT $DIR/curl.out

	$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

	assert_xml_value "project-commit" '/scmProjectActionInput/actionId' $DIR/curl.out
	assert_xml_value "export" '/scmProjectActionInput/integration' $DIR/curl.out
	assert_xml_value "Commit Changes to Git" '/scmProjectActionInput/title' $DIR/curl.out
	assert_xml_value "3" 'count(/scmProjectActionInput/fields/scmPluginInputField)' $DIR/curl.out
	assert_xml_value "Commit Message" '/scmProjectActionInput/fields/scmPluginInputField[name="message"]/title' $DIR/curl.out

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

main(){
	test_export_actions_fields_xml "testscm1"
	#test_export_perform_action_xml "testscm2"

	test_export_actions_fields_json "testscm3"
	#test_export_perform_action_json "testscm4"
}

main