#!/bin/bash


#test api request header version required.

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

# now submit req
runurl="${APIURL}/projects"

#
# TEST: request without version number
#

echo "TEST: require version header"

# get listing
$CURL ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

sh $DIR/api-test-error.sh $DIR/curl.out "RunDeck API Version not specified" || exit 2
echo "OK"

#
# TEST: request with wrong version number
#

echo "TEST: version number != 1.2"

# get listing
$CURL --header "X-RUNDECK-API-VERSION: 1.3" ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

sh $DIR/api-test-error.sh $DIR/curl.out "RunDeck API Version is not supported: 1.3" || exit 2
echo "OK"


rm $DIR/curl.out

