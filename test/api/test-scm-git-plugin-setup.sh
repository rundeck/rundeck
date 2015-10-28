#!/bin/bash

#/ Purpose:
#/   Test the scm plugins api setup methods
#/ 

DIR=$(cd `dirname $0` && pwd)
export API_XML_NO_WRAPPER=1
source $DIR/include.sh
#set -x

proj=$2
if [ "" == "$2" ] ; then
    proj="test"
fi

baregitfile=$DIR/git-bare-init.zip

setup_remote(){
	local gitdir=$1
	cd $gitdir
	unzip $baregitfile
}
remove_remote(){
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
	tmp=$DIR/test_setup_export_xml-upload.xml
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
test_setup_export_json_invalid_config(){
	local integration=$1
	local plugin=$2
	local dirval=$3
	local urlval=$4
	local msg=$5

	ENDPOINT="${APIURL}/project/$proj/scm/$integration/plugin/$plugin/setup"
	tmp=$DIR/test_setup_export_xml-upload.json
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
disable_plugin(){
	local integration=$1

	METHOD=POST
	ACCEPT=application/json
	TYPE=application/json
	POSTFILE=$tmp

	ENDPOINT="${APIURL}/project/$proj/scm/$integration/disable"
	api_request $ENDPOINT $DIR/curl.out


}

main(){
	test_setup_export_xml_invalid_config "export" "git-export" "" "" "two missing params"
	test_setup_export_xml_invalid_config "export" "git-export" "" "asd" "dir missing"
	test_setup_export_xml_invalid_config "export" "git-export" "asd" "" "url missing"
	
	test_setup_export_json_invalid_config "export" "git-export" "" "" "two missing params"
	test_setup_export_json_invalid_config "export" "git-export" "" "abc" "dir missing"
	test_setup_export_json_invalid_config "export" "git-export" "abc" "" "url missing"

}

main