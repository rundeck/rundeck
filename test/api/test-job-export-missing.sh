#!/bin/bash

#test output from /api/job/{id} when job ID does not exist

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

###
# Export an invalid ID
###

echo "TEST: export job with wrong ID"


# now submit req
runurl="${apiurl}/job/9000"
params=""

# expect error message
sh $DIR/api-expect-error.sh "${runurl}" "${params}" "Job ID does not exist: 9000" || exit 2
echo "OK"


rm $DIR/curl.out

