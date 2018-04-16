#!/bin/bash

#/ Purpose:
#/   Test the scm plugins inputs endpoints
#/ 

DIR=$(cd `dirname $0` && pwd)
export API_XML_NO_WRAPPER=1
source $DIR/include.sh
#set -x

proj=$2
if [ "" == "$2" ] ; then
    proj="test"
fi

#/ props for input plugin
commonprops="pathTemplate
dir
url
branch
strictHostKeyChecking
sshPrivateKeyPath
gitPasswordPath
format
fetchAutomatically
"
props="importUuidBehavior
pullAutomatically
useFilePattern
filePattern
$commonprops
"
pcount=13

#/ additional props for export plugin
exprops="committerName
committerEmail
exportUuidBehavior
pullAutomatically
$commonprops
"
expcount=13

test_git_plugin_input_xml(){
	local integration=$1
	local plugintype=$2


	ENDPOINT="${APIURL}/project/$proj/scm/$integration/plugin/$plugintype/input"
	
	test_begin "XML Response: $integration plugin inputs for $plugintype"

	api_request $ENDPOINT $DIR/curl.out

	$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

	assert_xml_value "$integration" '/scmPluginSetupInput/integration' $DIR/curl.out
	assert_xml_value "$plugintype" '/scmPluginSetupInput/type' $DIR/curl.out

	if [ "$integration" == "export" ] ; then
		assert_xml_value "$expcount" 'count(/scmPluginSetupInput/fields/scmPluginInputField)' $DIR/curl.out

		for prop in $exprops ; do
			assert_xml_value "$prop" "/scmPluginSetupInput/fields/scmPluginInputField[name='$prop']/name" $DIR/curl.out
		done
	
	else
		assert_xml_value "$pcount" 'count(/scmPluginSetupInput/fields/scmPluginInputField)' $DIR/curl.out

		for prop in $props ; do
			assert_xml_value "$prop" "/scmPluginSetupInput/fields/scmPluginInputField[name='$prop']/name" $DIR/curl.out
		done
	fi
	

	
	test_succeed

}

test_git_plugin_input_json(){
	local integration=$1
	local plugintype=$2


	ENDPOINT="${APIURL}/project/$proj/scm/$integration/plugin/$plugintype/input"
	ACCEPT=application/json
	
	test_begin "XML Response: $integration plugin fields for $plugintype"

	api_request $ENDPOINT $DIR/curl.out

	
	assert_json_value "$integration" '.integration' $DIR/curl.out
	assert_json_value "$plugintype" '.type' $DIR/curl.out
	local pnames=$props

	if [ "$integration" == "export" ] ; then
		assert_json_value "$expcount" '.fields | length' $DIR/curl.out
		pnames=$exprops
	else
		assert_json_value "$pcount" '.fields | length' $DIR/curl.out
	fi
	

	assert_json_value "$pnames" ".fields[].name" $DIR/curl.out
	
	test_succeed

}

main(){
	test_git_plugin_input_xml "import" "git-import"
	test_git_plugin_input_xml "export" "git-export"
	test_git_plugin_input_json "import" "git-import"
	test_git_plugin_input_json "export" "git-export"
}

main