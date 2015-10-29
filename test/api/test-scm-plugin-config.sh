#!/bin/bash

#/ Purpose:
#/   Test the scm plugins api setup methods
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
test_plugin_config_xml(){
	local integration=$1
	local plugin=$2
	local project=$3

	ENDPOINT="${APIURL}/project/$project/scm/$integration/config"
	
	test_begin "Setup SCM Config: XML"

	create_project $project
	
	do_setup_export_json_valid $integration $plugin $project
	
	METHOD=GET
	ACCEPT=application/xml
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/project/$project/scm/$integration/config"
	api_request $ENDPOINT $DIR/curl.out

	assert_xml_value "$integration" '/scmProjectPluginConfig/integration' $DIR/curl.out
	assert_xml_value "$project" '/scmProjectPluginConfig/project' $DIR/curl.out
	assert_xml_value "$plugin" '/scmProjectPluginConfig/type' $DIR/curl.out
	assert_xml_value "true" '/scmProjectPluginConfig/enabled' $DIR/curl.out
	assert_xml_value "8" 'count(/scmProjectPluginConfig/config/entry)' $DIR/curl.out
	assert_xml_value "yes" '/scmProjectPluginConfig/config/entry[@key="strictHostKeyChecking"]' $DIR/curl.out
	assert_xml_value "master" '/scmProjectPluginConfig/config/entry[@key="branch"]' $DIR/curl.out
	assert_xml_value "Git Test" '/scmProjectPluginConfig/config/entry[@key="committerName"]' $DIR/curl.out
	assert_xml_value "A@test.com" '/scmProjectPluginConfig/config/entry[@key="committerEmail"]' $DIR/curl.out
	assert_xml_value "xml" '/scmProjectPluginConfig/config/entry[@key="format"]' $DIR/curl.out
	assert_xml_value '${job.group}${job.name}-${job.id}.${config.format}' '/scmProjectPluginConfig/config/entry[@key="pathTemplate"]' $DIR/curl.out
	assert_xml_notblank '/scmProjectPluginConfig/config/entry[@key="dir"]' $DIR/curl.out
	assert_xml_notblank '/scmProjectPluginConfig/config/entry[@key="url"]' $DIR/curl.out

	test_succeed

	remove_project $project
}

test_plugin_config_json(){
local integration=$1
	local plugin=$2
	local project=$3

	ENDPOINT="${APIURL}/project/$project/scm/$integration/config"
	
	test_begin "Setup SCM Config: JSON"

	create_project $project
	
	do_setup_export_json_valid $integration $plugin $project
	
	METHOD=GET
	ACCEPT=application/json
	EXPECT_STATUS=200
	ENDPOINT="${APIURL}/project/$project/scm/$integration/config"
	api_request $ENDPOINT $DIR/curl.out

	assert_json_value "$integration" '.integration' $DIR/curl.out
	assert_json_value "$project" '.project' $DIR/curl.out
	assert_json_value "$plugin" '.type' $DIR/curl.out
	assert_json_value "true" '.enabled' $DIR/curl.out
	assert_json_value "8" '.config | length' $DIR/curl.out
	assert_json_value "yes" '.config["strictHostKeyChecking"]' $DIR/curl.out
	assert_json_value "master" '.config["branch"]' $DIR/curl.out
	assert_json_value "Git Test" '.config["committerName"]' $DIR/curl.out
	assert_json_value "A@test.com" '.config["committerEmail"]' $DIR/curl.out
	assert_json_value "xml" '.config["format"]' $DIR/curl.out
	assert_json_value '${job.group}${job.name}-${job.id}.${config.format}' '.config["pathTemplate"]' $DIR/curl.out
	assert_json_not_null '.config["dir"]' $DIR/curl.out
	assert_json_not_null '.config["url"]' $DIR/curl.out

	test_succeed

	remove_project $project

}


main(){
	test_plugin_config_xml "export" "git-export" "testscm1"
	test_plugin_config_json "export" "git-export" "testscm2"

}

main