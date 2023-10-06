#!/bin/bash

# usage:
#  api-test-error.sh <file> <message>
# Tests json file : expects result error="true", with result message if specified

errorMsg() {
   echo "$*" 1>&2
}

SRC_DIR=$(cd `dirname $0` && pwd)
DIR=${TMP_DIR:-$SRC_DIR}

JQ=$(which jq)

file=$1
shift

message="$*"

# test json
json=$($JQ  "." < "$file" )
jsontest=$?

if [ $jsontest != 0 ] ; then
    errorMsg "FAIL: Response was not valid json"
    exit 2
fi

waserror=$($JQ -r ".error" < "${file}")
if [ "true" != "$waserror" ] ; then
    errorMsg "FAIL: expected error result but was successful: ${message}"
    exit 2
fi
if [ "" != "${message}" ] ; then
    errmsg=$($JQ -r ".message" < ${file})
    if [ "${errmsg}" != "${message}" ] ; then
        errorMsg "FAIL: wrong error message: ${errmsg}, expected ${message}"
        exit 2
    fi
fi
exit 0
