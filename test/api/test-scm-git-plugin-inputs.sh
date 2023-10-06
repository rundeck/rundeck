#!/bin/bash

#/ Purpose:
#/   Test the scm plugins inputs endpoints
#/ 

DIR=$(cd `dirname $0` && pwd)
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
createBranch
baseBranch
$commonprops
"
expcount=15


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
	test_git_plugin_input_json "import" "git-import"
	test_git_plugin_input_json "export" "git-export"
}

main