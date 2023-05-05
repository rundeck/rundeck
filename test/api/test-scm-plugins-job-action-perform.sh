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


test_job_action_perform_xml(){
	local project=$1
	local integration=export

	local JOBID=$(setup_export_job $project)
	local actionId="job-commit"
	
	local commitMessage="a test commit"

	sleep 2


	METHOD=POST
	ACCEPT=application/xml
	TYPE=application/xml
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/job/$JOBID/scm/$integration/action/$actionId"

	TMPDIR=`tmpdir`
	tmp=$TMPDIR/commit.xml
	cat >$tmp <<END
<scmAction>
	<input>
		<entry key="message">$commitMessage</entry>
	</input>
</scmAction>
END
	POSTFILE=$tmp


	test_begin "SCM Job Action Perform (xml)"
	api_request $ENDPOINT $DIR/curl.out

	$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

	assert_xml_value "true" '/scmActionResult/success' $DIR/curl.out
	assert_xml_value "SCM export Action was Successful: $actionId" '/scmActionResult/message' $DIR/curl.out


	test_succeed

	remove_project $project
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
	test_job_action_perform_xml "testscm1"
	test_job_action_perform_json "testscm2"
}

main