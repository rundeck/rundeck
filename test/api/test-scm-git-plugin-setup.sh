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

proj="test"

baregitfile=$DIR/git-bare-init.zip

setup_remote(){
	local gitdir=$1
	mkdir -p $gitdir
	cd $gitdir
	unzip -q $baregitfile -d $gitdir
}
remove_testdir(){
	local gitdir=$1
	if [ -d $gitdir/.git ] ; then
		rm -r $gitdir
	fi
}

test_setup_export_xml_invalid_config(){
	local integration=$1
	local plugin=$2
	local dirval=$3
	local urlval=$4
	local msg=$5

	ENDPOINT="${APIURL}/project/$proj/scm/$integration/plugin/$plugin/setup"
	TMPDIR=`tmpdir`
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
	TMPDIR=`tmpdir`
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

do_setup_export_json_valid(){
	local integration=$1
	local plugin=$2
	local project=$3

	ENDPOINT="${APIURL}/project/$proj/scm/$integration/plugin/$plugin/setup"

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
test_setup_export_json_valid(){
	local integration=$1
	local plugin=$2
	local project=$3
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

test_disable_export_json(){
	local project=$1
	
	test_begin "Disable plugin"

	do_setup_export_json_valid "export" "git-export" $project
	
	disable_plugin_json "export" "git-export" $project
	
	test_succeed
}

test_disable_export_xml(){
	local project=$1
	
	test_begin "Disable plugin"

	do_setup_export_json_valid "export" "git-export" $project
	
	disable_plugin_xml "export" "git-export" $project
	
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
	
	# disable using xml
	create_project "testscm4"
	test_disable_export_xml "testscm4"
	remove_project "testscm4"
}

main