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
$CURL -D $DIR/headers.out ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#test curl.out for valid xml
$XMLSTARLET val -w $DIR/curl.out > /dev/null 2>&1
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response was not valid xml"
    exit 2
fi

#test for expected /joblist element
$XMLSTARLET el $DIR/curl.out | grep -e '^result' -q
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response did not contain expected result"
    exit 2
fi

#
# EXPECT message: "RunDeck API Version not specified"
#

# job list query doesn't wrap result in common result wrapper
#If <result error="true"> then an error occured.
waserror=$($XMLSTARLET sel -T -t -v "/result/@error" $DIR/curl.out)
errmsg=$($XMLSTARLET sel -T -t -v "/result/error/message" $DIR/curl.out)
if [ "true" == "$waserror" -a "RunDeck API Version not specified" == "$errmsg" ] ; then
    echo "OK"
else
    errorMsg "TEST FAILED: Version required message was expected"
    exit 2
fi

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

#test curl.out for valid xml
$XMLSTARLET val -w $DIR/curl.out > /dev/null 2>&1
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response was not valid xml"
    exit 2
fi

#test for expected /joblist element
$XMLSTARLET el $DIR/curl.out | grep -e '^result' -q
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response did not contain expected result"
    exit 2
fi

#
# EXPECT message: "RunDeck API Version not supported"
#

# job list query doesn't wrap result in common result wrapper
#If <result error="true"> then an error occured.
waserror=$($XMLSTARLET sel -T -t -v "/result/@error" $DIR/curl.out)
errmsg=$($XMLSTARLET sel -T -t -v "/result/error/message" $DIR/curl.out)
if [ "true" == "$waserror" -a "RunDeck API Version is not supported: 1.3" == "$errmsg" ] ; then
    echo "OK"
    exit 0
else
    errorMsg "TEST FAILED: Version invalid message was expected: $errmsg"
    exit 2
fi


rm $DIR/curl.out

