#!/bin/bash

#test output from /api/system/info

# don't use xml wrapper
API_XML_NO_WRAPPER=1

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh



####
# Test:
####

# now submit req
runurl="${APIURL}/system/info"

echo "TEST: ${runurl} (default xml output) ..."

params=""

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi


#Check projects list
testapivers=$($XMLSTARLET sel -T -t -v "/system/rundeck/apiversion" $DIR/curl.out)
assert "${API_VERSION}" "${testapivers}" "Expected latest api version"

echo "OK"

runurl="${APIURL}/system/info"
params="format=xml"

echo "TEST: ${runurl}?${params} ..."


# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi


#Check projects list
testapivers=$($XMLSTARLET sel -T -t -v "/system/rundeck/apiversion" $DIR/curl.out)
assert "${API_VERSION}" "${testapivers}" "Expected latest api version"

echo "OK"


runurl="${APIURL}/system/info"

echo "TEST: ${runurl} (accept:xml) ..."

params=""

# get listing
docurl -H 'accept:application/xml' ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi


#Check projects list
testapivers=$($XMLSTARLET sel -T -t -v "/system/rundeck/apiversion" $DIR/curl.out)
assert "${API_VERSION}" "${testapivers}" "Expected latest api version"

echo "OK"

runurl="${APIURL}/system/info"
params="format=json"

echo "TEST: ${runurl}?${params} ..."


# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

assert_json_value "${API_VERSION}" ".system.rundeck.apiversion" $DIR/curl.out || exit 2


echo "OK"

runurl="${APIURL}/system/info"
params=""

echo "TEST: ${runurl}?${params} (accept:json)..."


# get listing
docurl -H 'Accept:application/json' ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

assert_json_value "${API_VERSION}" ".system.rundeck.apiversion" $DIR/curl.out || exit 2


echo "OK"


#rm $DIR/curl.out

