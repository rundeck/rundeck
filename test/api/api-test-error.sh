#!/bin/bash

# usage:
#  api-test-error.sh <file> <message>
# Tests xml file : expects result error="true", with result message if specified

errorMsg() {
   echo "$*" 1>&2
}

SRC_DIR=$(cd `dirname $0` && pwd)
DIR=${TMP_DIR:-$SRC_DIR}

XMLSTARLET=${XMLSTARLET:-xmlstarlet}

file=$1
shift

message="$*"

#test curl.out for valid xml
$XMLSTARLET val -w ${file} > /dev/null 2>&1
if [ 0 != $? ] ; then
    errorMsg "FAIL: Response was not valid xml"
    exit 2
fi

#test for expected /joblist element
$XMLSTARLET el ${file} | grep -e '^result' -q
if [ 0 != $? ] ; then
    errorMsg "FAIL: Response did not contain expected result"
    exit 2
fi

#If <result error="true"> then an error occured.
waserror=$($XMLSTARLET sel -T -t -v "/result/@error" ${file})
if [ "true" != "$waserror" ] ; then
    errorMsg "FAIL: expected error result but was successful: ${message}"
    exit 2
fi
if [ "" != "${message}" ] ; then 
    errmsg=$($XMLSTARLET sel -T -t -v "/result/error/message" ${file})
    if [ "${errmsg}" != "${message}" ] ; then
        errorMsg "FAIL: wrong error message: ${errmsg}, expected ${message}"
        exit 2
    fi
fi

exit 0