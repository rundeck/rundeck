#!/bin/bash

#test update project resources file

DIR=$(cd `dirname $0` && pwd)
export API_VERSION=2 #/api/2/project/NAME/resources
source $DIR/include.sh

file=$DIR/curl.out

# now submit req
proj="test"

runurl="${APIURL}/project/${proj}/resources"

echo "TEST: /api/2/project/${proj}/resources (GET)"
params="format=xml"

# get listing
docurl ${runurl}?${params} > ${file} || fail "ERROR: failed request"

#test curl.out for valid xml
$XMLSTARLET val -w ${file} > /dev/null 2>&1
validxml=$?
if [ 0 == $validxml ] ; then 
    #test for for possible result error message
    $XMLSTARLET el ${file} | grep -e '^result' -q
    if [ 0 == $? ] ; then
        #test for error message
        #If <result error="true"> then an error occured.
        waserror=$($XMLSTARLET sel -T -t -v "/result/@error" ${file})
        errmsg=$($XMLSTARLET sel -T -t -v "/result/error/message" ${file})
        if [ "" != "$waserror" -a "true" == $waserror ] ; then
            errorMsg "FAIL: expected resource.xml content but received error result: $errmsg"
            exit 2
        fi
    fi
fi

#test curl.out for valid xml
if [ 0 != $validxml ] ; then
    errorMsg "ERROR: Response was not valid xml"
    exit 2
fi

#test for expected /joblist element
$XMLSTARLET el ${file} | grep -e '^project' -q
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response did not contain expected result"
    exit 2
fi

#Check results list
itemcount=$($XMLSTARLET sel -T -t -v "count(/project/node)" ${file})
if [ "0" == "$itemcount" ] ; then
    errorMsg "FAIL: expected multiple /project/node element"
    exit 2
fi

echo "OK"


echo "TEST: /api/2/project/${proj}/resources (GET) (YAML)"
params="format=yaml"

# get listing
docurl ${runurl}?${params} > ${file} || fail "ERROR: failed request"

#test curl.out for valid xml
$XMLSTARLET val -w ${file} > /dev/null 2>&1
validxml=$?
if [ 0 == $validxml ] ; then
    fail "Should not be XML"
fi

grep -e 'nodename:' -q ${file}
if [ 0 != $? ] ; then
    fail "Result does not appear correct"
fi

echo "OK"


echo "TEST: $API_BASE/api/2/project/${proj}/resources (GET) (unsupported)"
params="format=unsupported"

docurl ${runurl}?${params} > ${file} || fail "ERROR: failed request"
$SHELL $SRC_DIR/api-test-error.sh ${file} "Unsupported API Version \"2\". API Request: $API_BASE/api/2/project/${proj}/resources. Reason: Minimum supported version: 3" || fail "ERROR: failed request"

echo "OK"

echo "TEST: /api/3/project/${proj}/resources (GET) (otherformat)"

API3URL="${RDURL}/api/3"
runurl3="${API3URL}/project/${proj}/resources"
params="format=other"

docurl ${runurl3}?${params} > ${file} || fail "ERROR: failed request"
$SHELL $SRC_DIR/api-test-error.sh ${file} "The format specified is unsupported: other" || fail "ERROR: failed request"

echo "OK"


rm $DIR/curl.out

