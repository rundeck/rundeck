#!/bin/bash

#/ Purpose:
#/   Test the scm plugins list endpoints
#/ 

DIR=$(cd `dirname $0` && pwd)
export API_XML_NO_WRAPPER=1
source $DIR/include.sh


proj=$2
if [ "" == "$2" ] ; then
    proj="test"
fi

test_plugins_list_xml(){
	local intname=$1
	ENDPOINT="${APIURL}/project/$proj/scm/$intname/plugins"

	test_begin "XML Response"

	ACCEPT=application/xml

	api_request $ENDPOINT $DIR/curl.out

	$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

	#Check projects list
	assert_xml_value $intname '/scmPluginList/integration' $DIR/curl.out
	assert_xml_value '1' 'count(/scmPluginList/plugins/scmPluginDescription)' $DIR/curl.out
	assert_xml_value "git-$intname" '/scmPluginList/plugins/scmPluginDescription/type' $DIR/curl.out

	test_succeed

}
#/ expect invalid integration name
test_plugins_list_xml_failure(){
	local intname=$1
	ENDPOINT="${APIURL}/project/$proj/scm/$intname/plugins"

	test_begin "XML Response/Invalid integration"

	ACCEPT=application/xml

	api_request $ENDPOINT $DIR/curl.out

	$SHELL $SRC_DIR/api-test-error.sh $DIR/curl.out \
	"Invalid API Request: the value \"$intname\" for parameter \"integration\" was invalid. It must be in the list: [export, import]" || exit 2

	test_succeed

}

test_plugins_list_json(){
	local intname=$1
	ENDPOINT="${APIURL}/project/$proj/scm/$intname/plugins"

	test_begin "JSON response"

	ACCEPT=application/json

	api_request $ENDPOINT $DIR/curl.out
	

	#Check projects list
	assert_json_value $intname '.integration' $DIR/curl.out
	assert_json_value '1' '.plugins | length' $DIR/curl.out
	assert_json_value "git-$intname" '.plugins[0].type' $DIR/curl.out

	test_succeed

}
test_plugins_list_json_failure(){
	local intname=$1
	ENDPOINT="${APIURL}/project/$proj/scm/$intname/plugins"

	test_begin "JSON response/Invalid integration"

	ACCEPT=application/json

	api_request $ENDPOINT $DIR/curl.out

	#Check projects list
	assert_json_value 'true' '.error' $DIR/curl.out
	assert_json_value "Invalid API Request: the value \"$intname\" for parameter \"integration\" was invalid. It must be in the list: [export, import]" \
	 '.message' $DIR/curl.out

	test_succeed

}

main(){
	test_plugins_list_xml 'import'
	test_plugins_list_xml 'export'
	test_plugins_list_xml_failure 'invalid'
	test_plugins_list_json 'import'
	test_plugins_list_json 'export'
	test_plugins_list_json_failure 'invalid'
}

main