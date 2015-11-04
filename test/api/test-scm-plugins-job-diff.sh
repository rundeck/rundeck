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

	api_request $ENDPOINT $DIR/curl.out

	$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2
}

test_job_export_diff_clean_xml(){
	local project=$1
	local integration=export

	local JOBID=$(setup_export_job $project)
	
	perform_job_action $project "$integration" "job-commit" "$JOBID"

	sleep 2

	#get job diff clean

	METHOD=GET
	ACCEPT=application/xml
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/job/$JOBID/scm/$integration/diff"
	test_begin "SCM Job Diff clean (xml)"
	api_request $ENDPOINT $DIR/curl.out

	$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

	assert_xml_notblank '/scmJobDiff/commit/commitId' $DIR/curl.out
	assert_xml_value "" '/scmJobDiff/diffContent' $DIR/curl.out
	assert_xml_value "$JOBID" '/scmJobDiff/id' $DIR/curl.out
	assert_xml_value "$integration" '/scmJobDiff/integration' $DIR/curl.out
	assert_xml_value "$project" '/scmJobDiff/project' $DIR/curl.out

	test_succeed

	remove_project $project
}
test_job_export_diff_modified_xml(){
	local project=$1
	local integration=export

	local JOBID=$(setup_export_job $project)
	
	perform_job_action $project "$integration" "job-commit" "$JOBID"

	modify_job $project "$JOBID"

	#get job diff clean

	METHOD=GET
	ACCEPT=application/xml
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/job/$JOBID/scm/$integration/diff"
	test_begin "SCM Job Diff modified (xml)"
	api_request $ENDPOINT $DIR/curl.out

	$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

	assert_xml_notblank '/scmJobDiff/commit/commitId' $DIR/curl.out
	assert_xml_notblank '/scmJobDiff/diffContent' $DIR/curl.out
	assert_xml_value "$JOBID" '/scmJobDiff/id' $DIR/curl.out
	assert_xml_value "$integration" '/scmJobDiff/integration' $DIR/curl.out
	assert_xml_value "$project" '/scmJobDiff/project' $DIR/curl.out

	test_succeed

	remove_project $project
}


test_job_export_diff_clean_json(){
	local project=$1
	local integration=export

	local JOBID=$(setup_export_job $project)
	
	perform_job_action $project "$integration" "job-commit" "$JOBID"

	sleep 2

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
	
	perform_job_action $project "$integration" "job-commit" "$JOBID"

	modify_job $project "$JOBID"

	sleep 2

	#get job diff clean

	METHOD=GET
	ACCEPT=application/json
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/job/$JOBID/scm/$integration/diff"
	test_begin "SCM Job Diff clean (json)"
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
	test_job_export_diff_clean_xml "testscm1"
	test_job_export_diff_clean_json "testscm2"

	test_job_export_diff_modified_xml "testscm3"
	test_job_export_diff_modified_json "testscm4"
}

main