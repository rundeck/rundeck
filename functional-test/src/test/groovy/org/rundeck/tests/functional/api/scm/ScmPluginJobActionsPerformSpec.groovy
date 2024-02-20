package org.rundeck.tests.functional.api.scm

import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.scm.GitScmApiClient
import org.rundeck.util.api.scm.httpbody.GitExportSetupRequest
import org.rundeck.util.api.scm.httpbody.ScmActionPerformRequest
import org.rundeck.util.api.scm.httpbody.SetupIntegrationResponse
import org.rundeck.util.container.BaseContainer

@APITest
class ScmPluginJobActionsPerformSpec extends BaseContainer{


    static final String PROJECT_NAME = "ScmPluginJobActionsPerform-project"
    final String EXPORT_INTEGRATION= "export"
    final String DUMMY_JOB_ID = "383d0599-3ea3-4fa6-ac3a-75a53d6bfdf3"
    final String JOB_XML_LOCATION = "/Users/jesus/rundeckpro/rundeck/functional-test/src/test/resources/projects-import/scm/dummy-job-scm-action-perform.xml"

    def setupSpec() {
        startEnvironment()
        setupProject(PROJECT_NAME)
    }

    def "test_job_action_perform"(){
        given:
        setupProject(PROJECT_NAME)
        jobImportFile(PROJECT_NAME,JOB_XML_LOCATION)
        GitScmApiClient scmClient = new GitScmApiClient(clientProvider).forIntegration(EXPORT_INTEGRATION).forProject(PROJECT_NAME)
        scmClient.callSetupIntegration(GitExportSetupRequest.defaultRequest(PROJECT_NAME))
        ScmActionPerformRequest actionRequest = new ScmActionPerformRequest([
                input: [ message : "Commit msg example" ],
                jobs: [ DUMMY_JOB_ID ]
        ])
        def actionId = 'job-commit'


        when:
        SetupIntegrationResponse performAction = scmClient.callPerformJobAction(actionId,actionRequest,DUMMY_JOB_ID).response

        then:
        verifyAll {
            performAction.message == "SCM export Action was Successful: ${actionId}"
            performAction.success == true
        }




    }
}

/*
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




test_job_action_perform_json(){
    local project=$1
    local integration=export

    local JOBID=$(setup_export_job $project)
    local actionId="job-commit"
    local commitMessage="test commit message"

    sleep 2

    #get job status

    METHOD=POST
    ACCEPT=application/json
    TYPE=application/json
    EXPECT_STATUS=200
    ENDPOINT="${APIURL}/job/$JOBID/scm/$integration/action/$actionId"

    TMPDIR=`tmpdir`
    tmp=$TMPDIR/commit.json
    cat >$tmp <<END
    {
        "input":{
        "message":"$commitMessage"
    }
    }
    END
    POSTFILE=$tmp

    test_begin "SCM Job Action Input (json)"

    api_request $ENDPOINT $DIR/curl.out


    assert_json_value "true" '.success' $DIR/curl.out
    assert_json_value "SCM export Action was Successful: $actionId" '.message' $DIR/curl.out

    test_succeed

    remove_project $project
}

main(){
    test_job_action_perform_json "testscm2"
}

main */