#!/bin/bash

#/ Purpose:
#/   Test the scm plugins api setup methods
#/ 

SRC_DIR=$(cd `dirname $0` && pwd)
DIR=${TMP_DIR:-$SRC_DIR}
export API_XML_NO_WRAPPER=1
source $SRC_DIR/include_scm_test.sh

ARGS=$@


proj="test"

test_setup_export_xml_invalid_config(){
	local integration=$1
	local plugin=$2
	local dirval=$3
	local urlval=$4
	local msg=$5

	ENDPOINT="${APIURL}/project/$proj/scm/$integration/plugin/$plugin/setup"
	local TMPDIR=`tmpdir`
	tmp=$TMPDIR/test_setup_export_xml-upload.xml
	cat >$tmp <<END
<scmPluginConfig>
<config>
	<entry key="dir">$dirval</entry>
	<entry key="url">$urlval</entry>
</config>
</scmPluginConfig>
END
	METHOD=POST
	ACCEPT=application/xml
	TYPE=application/xml
	POSTFILE=$tmp
	EXPECT_STATUS=400

	test_begin "Setup SCM Export: XML: $msg"
	
	api_request $ENDPOINT $DIR/curl.out

	$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2


	assert_xml_value "false" '/scmActionResult/success' $DIR/curl.out
	assert_xml_value "Some input values were not valid." '/scmActionResult/message' $DIR/curl.out
	if [ -z "$dirval" ] ; then
		assert_xml_value "required" '/scmActionResult/validationErrors/entry[@key="dir"]' $DIR/curl.out
	else
		assert_xml_value "" '/scmActionResult/validationErrors/entry[@key="dir"]' $DIR/curl.out
	fi

	if [ -z "$urlval" ] ; then
		assert_xml_value "required" '/scmActionResult/validationErrors/entry[@key="url"]' $DIR/curl.out
	else
		assert_xml_value "" '/scmActionResult/validationErrors/entry[@key="url"]' $DIR/curl.out
	fi

	test_succeed
}

test_setup_export_xml_valid(){
	local integration=$1
	local plugin=$2
	local project=$3
	
	ENDPOINT="${APIURL}/project/$project/scm/$integration/plugin/$plugin/setup"
	TMPDIR=`tmpdir`/$project
	mkdir -p $TMPDIR
	tmp=$TMPDIR/test_setup_export_xml-upload.xml
	dirname=$TMPDIR/testdir
	gitdir=$TMPDIR/testgit
	mkdir $dirname
	setup_remote $gitdir
	cat >$tmp <<END
<scmPluginConfig>
<config>
	<entry key="dir">$dirname</entry>
	<entry key="url">$gitdir</entry>
	<entry key="committerName">Git Test</entry>
	<entry key="committerEmail">A@test.com</entry>
	<entry key="pathTemplate">\${job.group}\${job.name}-\${job.id}.\${config.format}</entry>
	<entry key="format">xml</entry>
	<entry key="branch">master</entry>
	<entry key="strictHostKeyChecking">yes</entry>
</config>
</scmPluginConfig>
END
	METHOD=POST
	ACCEPT=application/xml
	TYPE=application/xml
	POSTFILE=$tmp
	EXPECT_STATUS=200

	test_begin "Setup SCM Export: XML: $msg"
	
	api_request $ENDPOINT $DIR/curl.out

	$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2


	assert_xml_value "true" '/scmActionResult/success' $DIR/curl.out
	assert_xml_value "SCM Plugin Setup Complete" '/scmActionResult/message' $DIR/curl.out
	
	remove_testdir $gitdir
	remove_testdir $dirname


	test_succeed
}
test_setup_export_json_invalid_config(){
	local integration=$1
	local plugin=$2
	local dirval=$3
	local urlval=$4
	local msg=$5

	ENDPOINT="${APIURL}/project/$proj/scm/$integration/plugin/$plugin/setup"
	TMPDIR=`tmpdir`
	tmp=$TMPDIR/test_setup_export_xml-upload.json
	cat >$tmp <<END
{
	"config":{
		"dir":"$dirval",
		"url":"$urlval"
	}
}
END
	METHOD=POST
	ACCEPT=application/json
	TYPE=application/json
	POSTFILE=$tmp
	EXPECT_STATUS=400

	test_begin "Setup SCM Export: JSON: $msg"
	
	api_request $ENDPOINT $DIR/curl.out

	assert_json_value "false" '.success' $DIR/curl.out
	assert_json_value "Some input values were not valid." '.message' $DIR/curl.out
	if [ -z "$dirval" ] ; then
		assert_json_value "required" '.validationErrors.dir' $DIR/curl.out
	else
		assert_json_null '.validationErrors.dir' $DIR/curl.out
	fi
	if [ -z "$urlval" ] ; then
		assert_json_value "required" '.validationErrors.url' $DIR/curl.out
	else
		assert_json_null '.validationErrors.url' $DIR/curl.out
	fi

	test_succeed
}

test_setup_export_json_valid(){
	local integration=$1
	local plugin=$2
	local project=$3
	ENDPOINT="${APIURL}/project/$project/scm/$integration/plugin/$plugin/setup"
	test_begin "Setup SCM Export: JSON"
	do_setup_export_json_valid $integration $plugin $project
	test_succeed
}

disable_plugin_xml(){
	local integration=$1
	local plugin=$2
	local project=$3

	METHOD=POST
	ACCEPT=application/xml
	ENDPOINT="${APIURL}/project/$project/scm/$integration/plugin/$plugin/disable"

	api_request $ENDPOINT $DIR/curl.out

	$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

	assert_xml_value "true" '/scmActionResult/success' $DIR/curl.out

}

enable_plugin_xml(){
	local integration=$1
	local plugin=$2
	local project=$3

	METHOD=POST
	ACCEPT=application/xml
	ENDPOINT="${APIURL}/project/$project/scm/$integration/plugin/$plugin/enable"

	api_request $ENDPOINT $DIR/curl.out

	$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

	assert_xml_value "true" '/scmActionResult/success' $DIR/curl.out

}
disable_plugin_json(){
	local integration=$1
	local plugin=$2
	local project=$3

	METHOD=POST
	ACCEPT=application/json
	ENDPOINT="${APIURL}/project/$project/scm/$integration/plugin/$plugin/disable"

	api_request $ENDPOINT $DIR/curl.out

	assert_json_value "true" '.success' $DIR/curl.out
}
enable_plugin_json(){
	local integration=$1
	local plugin=$2
	local project=$3

	METHOD=POST
	ACCEPT=application/json
	ENDPOINT="${APIURL}/project/$project/scm/$integration/plugin/$plugin/enable"

	api_request $ENDPOINT $DIR/curl.out

	assert_json_value "true" '.success' $DIR/curl.out
}
assert_plugin_enabled(){
	local integration=$1
	local plugin=$2
	local value=$3
	local project=$4

	ENDPOINT="${APIURL}/project/$project/scm/$integration/plugins"
	ACCEPT=application/xml

	api_request $ENDPOINT $DIR/curl.out

	$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

	#Check projects list
	assert_xml_value $integration '/scmPluginList/integration' $DIR/curl.out

	assert_xml_value "$value" "/scmPluginList/plugins/scmPluginDescription[type=\"$plugin\"]/enabled" $DIR/curl.out
}

test_disable_export_json(){
	local project=$1
	

	do_setup_export_json_valid "export" "git-export" $project

	assert_plugin_enabled "export" "git-export" "true" $project
	
	ENDPOINT="${APIURL}/project/$project/scm/export/plugin/git-export/disable"
	test_begin "Disable plugin JSON"
	
	disable_plugin_json "export" "git-export" $project
	
	assert_plugin_enabled "export" "git-export" "false" $project
	
	test_succeed
}
test_disable_export_wrong_json(){
	local project=$1
	

	do_setup_export_json_valid "export" "git-export" $project

	assert_plugin_enabled "export" "git-export" "true" $project
	
	ENDPOINT="${APIURL}/project/$project/scm/export/plugin/wrong-plugin/disable"
	test_begin "Disable plugin wrong type JSON"
	
	METHOD=POST
	ACCEPT=application/json
	EXPECT_STATUS=400
	
	api_request $ENDPOINT $DIR/curl.out

	assert_json_value "false" '.success' $DIR/curl.out
	
	assert_plugin_enabled "export" "git-export" "true" $project
	
	test_succeed
}
test_disable_export_nosetup_json(){
	local project=$1
	

	#do_setup_export_json_valid "export" "git-export" $project

	assert_plugin_enabled "export" "git-export" "false" $project
	
	ENDPOINT="${APIURL}/project/$project/scm/export/plugin/wrong-plugin/disable"
	test_begin "Disable plugin not enabled JSON"
	
	METHOD=POST
	ACCEPT=application/json
	EXPECT_STATUS=400
	
	api_request $ENDPOINT $DIR/curl.out

	assert_json_value "false" '.success' $DIR/curl.out
	
	assert_plugin_enabled "export" "git-export" "false" $project
	
	test_succeed
}
test_disable_enable_export_json(){
	local project=$1
	

	do_setup_export_json_valid "export" "git-export" $project

	assert_plugin_enabled "export" "git-export" "true" $project
	
	disable_plugin_json "export" "git-export" $project
	
	assert_plugin_enabled "export" "git-export" "false" $project
	

	ENDPOINT="${APIURL}/project/$project/scm/export/plugin/git-export/disable"
	test_begin "Enable plugin JSON"

	enable_plugin_json "export" "git-export" $project
	
	assert_plugin_enabled "export" "git-export" "true" $project
	
	test_succeed
}

test_disable_export_xml(){
	local project=$1
	

	do_setup_export_json_valid "export" "git-export" $project

	assert_plugin_enabled "export" "git-export" "true" $project
	

	ENDPOINT="${APIURL}/project/$project/scm/export/plugin/git-export/disable"
	test_begin "Disable plugin XML"

	disable_plugin_xml "export" "git-export" $project

	assert_plugin_enabled "export" "git-export" "false" $project
	
	test_succeed
}

test_disable_enable_export_xml(){
	local project=$1
	

	do_setup_export_json_valid "export" "git-export" $project

	assert_plugin_enabled "export" "git-export" "true" $project
	
	disable_plugin_xml "export" "git-export" $project

	assert_plugin_enabled "export" "git-export" "false" $project
	

	ENDPOINT="${APIURL}/project/$project/scm/export/plugin/git-export/enable"
	test_begin "Enable plugin XML"

	enable_plugin_xml "export" "git-export" $project

	assert_plugin_enabled "export" "git-export" "true" $project
	
	test_succeed
}

main(){
	test_setup_export_xml_invalid_config "export" "git-export" "" "" "two missing params"
	test_setup_export_xml_invalid_config "export" "git-export" "" "asd" "dir missing"
	test_setup_export_xml_invalid_config "export" "git-export" "asd" "" "url missing"

	test_setup_export_json_invalid_config "export" "git-export" "" "" "two missing params"
	test_setup_export_json_invalid_config "export" "git-export" "" "abc" "dir missing"
	test_setup_export_json_invalid_config "export" "git-export" "abc" "" "url missing"

	# setup using xml
	create_project "testscm1"
	test_setup_export_xml_valid "export" "git-export" "testscm1"
	remove_project "testscm1"

	# setup using json
	create_project "testscm2"
	test_setup_export_json_valid "export" "git-export" "testscm2"
	remove_project "testscm2"

	# disable using json
	create_project "testscm3"
	test_disable_export_json "testscm3"
	remove_project "testscm3"

	create_project "testscm3-2"
	test_disable_export_wrong_json "testscm3-2"
	remove_project "testscm3-2"

	create_project "testscm3-3"
	test_disable_export_nosetup_json "testscm3-3"
	remove_project "testscm3-3"
	
	# disable using xml
	create_project "testscm4"
	test_disable_export_xml "testscm4"
	remove_project "testscm4"

	# disable using json
	create_project "testscm5"
	test_disable_enable_export_json "testscm5"
	remove_project "testscm5"
	
	# disable using xml
	create_project "testscm6"
	test_disable_enable_export_xml "testscm6"
	remove_project "testscm6"
}

main