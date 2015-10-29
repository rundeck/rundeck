#!/bin/bash

#/ Purpose:
#/   Test the scm plugins api status
#/ 

DIR=$(cd `dirname $0` && pwd)
export API_XML_NO_WRAPPER=1
source $DIR/include.sh

ARGS=$@

tmpdir(){
	tempfoo=`basename $0`
	TMPDIR=`mktemp -d -t ${tempfoo}` || exit 1
	echo $TMPDIR
}

create_project(){
	local projectName=$1
	
	ENDPOINT="${APIURL}/projects"
	TMPDIR=`tmpdir`
	tmp=$TMPDIR/create-project.xml
	cat >$tmp <<END
<project>
    <name>$projectName</name>
</project>
END
	TYPE=application/xml
	POSTFILE=$tmp
	METHOD=POST
	EXPECT_STATUS=201
	api_request $ENDPOINT $DIR/curl.out
}
remove_project(){
	local projectName=$1

	ENDPOINT="${APIURL}/project/$projectName"
	METHOD=DELETE
	EXPECT_STATUS=204
	api_request $ENDPOINT $DIR/curl.out
}
baregitfile=$DIR/git-bare-init.zip

setup_remote(){
	local gitdir=$1
	mkdir -p $gitdir
	cd $gitdir
	unzip -q $baregitfile -d $gitdir
}
do_setup_export_json_valid(){
	local integration=$1
	local plugin=$2
	local project=$3

	ENDPOINT="${APIURL}/project/$project/scm/$integration/plugin/$plugin/setup"

	TMPDIR=`tmpdir`
	dirname=$TMPDIR/testdir
	gitdir=$TMPDIR/testgit
	mkdir $dirname
	setup_remote $gitdir

	tmp=$TMPDIR/test_setup_export_xml-upload.json
	cat >$tmp <<END
{
	"config":{
		"dir":"$dirname",
		"url":"$gitdir",
		"committerName":"Git Test",
		"committerEmail":"A@test.com",
		"pathTemplate":"\${job.group}\${job.name}-\${job.id}.\${config.format}",
		"format":"xml",
		"branch":"master",
		"strictHostKeyChecking":"yes"
	}
}
END
	METHOD=POST
	ACCEPT=application/json
	TYPE=application/json
	POSTFILE=$tmp
	EXPECT_STATUS=200

	
	api_request $ENDPOINT $DIR/curl.out

	assert_json_value "true" '.success' $DIR/curl.out
	assert_json_value "SCM Plugin Setup Complete" '.message' $DIR/curl.out
	
}
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
	test_plugin_status_xml "export" "git-export" "testscm1"
	test_plugin_status_json "export" "git-export" "testscm2"

}

main