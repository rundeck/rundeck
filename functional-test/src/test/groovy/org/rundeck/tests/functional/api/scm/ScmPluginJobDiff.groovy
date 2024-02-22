package org.rundeck.tests.functional.api.scm

import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.scm.GitScmApiClient
import org.rundeck.util.api.scm.httpbody.GitExportSetupRequest
import org.rundeck.util.api.scm.httpbody.ScmActionPerformRequest
import org.rundeck.util.api.scm.httpbody.ScmJobStatusResponse
import org.rundeck.util.api.scm.httpbody.SetupIntegrationResponse
import org.rundeck.util.container.BaseContainer

@APITest
class ScmPluginJobDiff extends BaseContainer{


    static final String PROJECT_NAME = "ScmPluginJobActionsInput-project"
    final String EXPORT_INTEGRATION = "export"
    final String DUMMY_JOB_ID = "383d0599-3ea3-4fa6-ac3a-75a53d6b3333"
    final String JOB_XML_NAME = "job-template-common.xml"

    def "test_job_action_perform"() {
        given:
        setupProject(PROJECT_NAME)
        jobImportFile(PROJECT_NAME, updateFile(JOB_XML_NAME,null,null,null,null,null,null,DUMMY_JOB_ID))
        GitScmApiClient scmClient = new GitScmApiClient(clientProvider).forIntegration(EXPORT_INTEGRATION).forProject(PROJECT_NAME)
        scmClient.callSetupIntegration(GitExportSetupRequest.defaultRequest(PROJECT_NAME))
        ScmJobStatusResponse firstStatus = scmClient.callGetJobStatus(DUMMY_JOB_ID).response
        ScmActionPerformRequest actionRequest = new ScmActionPerformRequest([
                input: [message: "Commit msg example"],
                jobs : [DUMMY_JOB_ID]
        ])
        def actionId = 'job-commit'

        when:
        SetupIntegrationResponse performAction = scmClient.callPerformJobAction(actionId, actionRequest, DUMMY_JOB_ID).response

        then:
        firstStatus.actions.size() == 1
        firstStatus.commit == null
        firstStatus.id == DUMMY_JOB_ID
        firstStatus.integration == EXPORT_INTEGRATION
        firstStatus.message == "Created"
        firstStatus.project == PROJECT_NAME
        firstStatus.synchState == "CREATE_NEEDED"

        performAction.message == "SCM export Action was Successful: ${actionId}"
        performAction.success == true

        ScmJobStatusResponse updatedStatus = scmClient.callGetJobStatus(DUMMY_JOB_ID).response
        updatedStatus.actions.size() == 0
        updatedStatus.commit.size()== 5
        updatedStatus.id == DUMMY_JOB_ID
        updatedStatus.integration == EXPORT_INTEGRATION
        updatedStatus.message == "No Change"
        updatedStatus.project == PROJECT_NAME
        updatedStatus.synchState == "CLEAN"
    }

}


/*

#!/bin/bash

#/ Purpose:
#/   Test the scm plugins api job diff
#/

DIR=$(cd `dirname $0` && pwd)

source $DIR/include_scm_test.sh

ARGS=$@

JOBNAME="job status test"

setup_export_job(){
	local project=$1

	create_project $project

	do_setup_export_json_valid "export" "git-export" $project

	JOBID=$(create_job $project "$JOBNAME" "original value")
	echo $JOBID
}
modify_job(){
	local project=$1
	local JOBID=$2

	local newjob=$(create_job $project "$JOBNAME" "different value")

	assert "$JOBID" "$newjob"
}

perform_job_action(){
	local project=$1
	local integration=$2
	local actionId=$3
	local JOBID=$4

	local commitMessage="blah"

	METHOD=POST
	ACCEPT=application/json
	TYPE=application/json
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/job/$JOBID/scm/$integration/action/$actionId"

	TMPDIR=`tmpdir`
	tmp=$TMPDIR/commit.xml
	cat >$tmp <<END
{
	"input":{
		"message":"$commitMessage"
	}
}
END
	POSTFILE=$tmp

	api_request $ENDPOINT $DIR/curl.out

	$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2
}
do_sleep(){
	sleep 2
}

assert_scm_job_status(){
	local project=$1
	local integration=$2
	local JOBID=$3
	local status=$4

	METHOD=GET
	ACCEPT=application/json
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/job/$JOBID/scm/$integration/status"
	api_request $ENDPOINT $DIR/curl.out

	assert_json_value "$integration" '.integration' $DIR/curl.out
	assert_json_value "$project" '.project' $DIR/curl.out
	assert_json_value "$status" '.synchState' $DIR/curl.out
}



test_job_export_diff_clean_json(){
	local project=$1
	local integration=export

	local JOBID=$(setup_export_job $project)


	do_sleep

	assert_scm_job_status $project "$integration" "$JOBID" "CREATE_NEEDED"

	perform_job_action $project "$integration" "job-commit" "$JOBID"

	#get job diff clean

	METHOD=GET
	ACCEPT=application/json
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/job/$JOBID/scm/$integration/diff"
	test_begin "SCM Job Diff clean (json)"
	api_request $ENDPOINT $DIR/curl.out



	assert_json_not_null '.commit.commitId' $DIR/curl.out
	assert_json_value "" '.diffContent' $DIR/curl.out
	assert_json_value "$JOBID" '.id' $DIR/curl.out
	assert_json_value "$integration" '.integration' $DIR/curl.out
	assert_json_value "$project" '.project' $DIR/curl.out

	test_succeed

	remove_project $project
}

test_job_export_diff_modified_json(){
	local project=$1
	local integration=export

	local JOBID=$(setup_export_job $project)

	do_sleep

	assert_scm_job_status $project "$integration" "$JOBID" "CREATE_NEEDED"

	perform_job_action $project "$integration" "job-commit" "$JOBID"

	modify_job $project "$JOBID"

	do_sleep

	assert_scm_job_status $project "$integration" "$JOBID" "EXPORT_NEEDED"

	#get job diff clean

	METHOD=GET
	ACCEPT=application/json
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/job/$JOBID/scm/$integration/diff"
	test_begin "SCM Job Diff modified (json)"
	api_request $ENDPOINT $DIR/curl.out



	assert_json_not_null '.commit.commitId' $DIR/curl.out
	assert_json_not_null '.diffContent' $DIR/curl.out
	assert_json_value "$JOBID" '.id' $DIR/curl.out
	assert_json_value "$integration" '.integration' $DIR/curl.out
	assert_json_value "$project" '.project' $DIR/curl.out

	test_succeed

	remove_project $project
}
main(){
	test_job_export_diff_clean_json "testscm-job-diff-2"

	test_job_export_diff_modified_json "testscm-job-diff-4"
}

main


 */