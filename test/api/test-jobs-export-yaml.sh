#!/bin/bash

#test output from /api/jobs/export using format=yaml

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

VERSHEADER="X-RUNDECK-API-VERSION: 1.2"

# curl opts to use a cookie jar, and follow redirects, showing only errors
CURLOPTS="-s -S -L -c $DIR/cookies -b $DIR/cookies"
CURL="curl $CURLOPTS"


XMLSTARLET=xml

# now submit req
runurl="${apiurl}/jobs/export"
proj=$2
if [ "" == "$2" ] ; then
    proj="test"
fi

echo "TEST: export RunDeck Jobs in jobs.yaml format"

params="project=${proj}&format=yaml"

# get listing
$CURL --header "$VERSHEADER" -D $DIR/headers.out ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#test headers.out for valid yaml content type
grep "Content-Type: text/yaml" $DIR/headers.out -q
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response was not yaml"
    exit 2
fi

#test yaml output for at least the id: entry
grep "id:" $DIR/curl.out -q
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response yaml did not have expected content"
    exit 2
fi
echo OK


rm $DIR/curl.out
rm $DIR/headers.out


