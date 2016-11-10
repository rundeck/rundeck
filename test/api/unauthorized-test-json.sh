#!/bin/bash

#test unauthorized access to: /api/1/projects

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

runurl="${APIURL}/projects"

#remove any existing cookies
test -f $DIR/cookies && rm $DIR/cookies

echo "TEST: unauthorized simple request (json)"

params=""

# get listing
$CURL  -D $DIR/headers.out -H 'accept:application/json' ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#test result error message

assert_json_value 'unauthorized' '.errorCode' $DIR/curl.out
assert_json_not_null '.message' $DIR/curl.out

echo "OK"

rm $DIR/curl.out
rm $DIR/headers.out


echo "TEST: unauthorized token request (header) (json)"

params=""
FAKEAUTH="abc123badtoken"

# get listing
$CURL -H "X-RunDeck-Auth-Token: $FAKEAUTH" -H 'accept:application/json' -D $DIR/headers.out ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#test result error message


assert_json_value 'unauthorized' '.errorCode' $DIR/curl.out
assert_json_not_null '.message' $DIR/curl.out

echo "OK"

rm $DIR/curl.out
rm $DIR/headers.out

echo "TEST: unauthorized token request (param) (json)"

params="authtoken=$FAKEAUTH"

# get listing
$CURL  -D $DIR/headers.out -H 'accept:application/json' ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#test result error message

assert_json_value 'unauthorized' '.errorCode' $DIR/curl.out
assert_json_not_null '.message' $DIR/curl.out
echo "OK"


rm $DIR/curl.out
rm $DIR/headers.out

