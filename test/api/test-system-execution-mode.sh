#!/bin/bash

#/ test setting executions active/passive mode.

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

test_disable_xml(){

	runurl="${APIURL}/system/executions/disable"
	ctype="-H accept:application/xml"

	echo "TEST: $runurl $ctype"

	docurl ${ctype} -X POST ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"
	assert_xml_valid $DIR/curl.out
	assert_xml_value "passive" "/executions/@executionMode" $DIR/curl.out
	echo "OK"
}

test_enable_xml(){

	runurl="${APIURL}/system/executions/enable"
	ctype="-H accept:application/xml"

	echo "TEST: $runurl $ctype"

	docurl ${ctype} -X POST ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"
	assert_xml_valid $DIR/curl.out
	assert_xml_value "active" "/executions/@executionMode" $DIR/curl.out
	echo "OK"
}


test_disable_json(){

	runurl="${APIURL}/system/executions/disable"
	ctype="-H accept:application/json"

	echo "TEST: $runurl $ctype"

	docurl ${ctype} -X POST ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"
	assert_json_value "passive" ".executionMode" $DIR/curl.out
	
	echo "OK"
}

test_enable_json(){
	
	runurl="${APIURL}/system/executions/enable"
	ctype="-H accept:application/json"

	echo "TEST: $runurl $ctype"
	
	docurl ${ctype} -X POST ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"
	assert_json_value "active" ".executionMode" $DIR/curl.out
	echo "OK"
}

test_all(){
	test_disable_xml
	test_enable_xml
	test_disable_json
	test_enable_json	
}

test_all