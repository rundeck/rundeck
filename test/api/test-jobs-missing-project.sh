#!/bin/bash

#test /api/jobs requires project parameter

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
runurl="${apiurl}/jobs"

echo "TEST: require project parameter for /api/jobs..."

params="project="

sh $DIR/api-expect-error.sh "${runurl}" "${params}" "parameter \"project\" is required" || exit 2
echo "OK"


rm $DIR/curl.out

