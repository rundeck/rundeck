#!/bin/bash

#test unauthorized access to: /api/1/projects

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

runurl="${APIURL}/projects"

#remove any existing cookies
test -f $DIR/cookies && rm $DIR/cookies

echo "TEST: unauthorized simple request (XML)"

params=""

# get listing
$CURL  -D $DIR/headers.out -H 'accept:application/xml'  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-error.sh $DIR/curl.out || exit 2

#test result error message

errcode=$(xmlsel "/result/error/@code" $DIR/curl.out)
errmsg=$(xmlsel "/result/error/message" $DIR/curl.out)
if [  "unauthorized" == "$errcode" ] ; then
    echo "OK"
else
    errorMsg "TEST FAILED: Unauthorized API Request message expected: $errmsg"
    exit 2
fi

rm $DIR/curl.out
rm $DIR/headers.out


echo "TEST: unauthorized token request (header) XML"

params=""
FAKEAUTH="abc123badtoken"

# get listing
$CURL -H "X-RunDeck-Auth-Token: $FAKEAUTH" -D $DIR/headers.out -H 'accept:application/xml'  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-error.sh $DIR/curl.out || exit 2

#test result error message

errcode=$(xmlsel "/result/error/@code" $DIR/curl.out)
errmsg=$(xmlsel "/result/error/message" $DIR/curl.out)
if [  "unauthorized" == "$errcode" ] ; then
    echo "OK"
else
    errorMsg "TEST FAILED: Unauthorized API Request message expected: $errmsg"
    exit 2
fi

rm $DIR/curl.out
rm $DIR/headers.out

echo "TEST: unauthorized token request (param) XML"

params="authtoken=$FAKEAUTH"

# get listing
$CURL  -D $DIR/headers.out -H 'accept:application/xml'  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-error.sh $DIR/curl.out || exit 2

#test result error message

errcode=$(xmlsel "/result/error/@code" $DIR/curl.out)
errmsg=$(xmlsel "/result/error/message" $DIR/curl.out)
if [  "unauthorized" == "$errcode" ] ; then
    echo "OK"
else
    errorMsg "TEST FAILED: Unauthorized API Request message expected: $errmsg"
    exit 2
fi

rm $DIR/curl.out
rm $DIR/headers.out

