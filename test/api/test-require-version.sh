#!/bin/bash


#test api request header version required.

errorMsg() {
   echo "$*" 1>&2
}

DIR=$(cd `dirname $0` && pwd)

# accept url argument on commandline, if '-' use default
url="$1"
if [ "-" == "$1" ] ; then
    url='http://localhost:4440/api'
fi
apiurl="${url}/api"

# curl opts to use a cookie jar, and follow redirects, showing only errors
CURLOPTS="-s -S -L -c $DIR/cookies -b $DIR/cookies"
CURL="curl $CURLOPTS"

XMLSTARLET=xml

# now submit req
runurl="${apiurl}/projects"

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

