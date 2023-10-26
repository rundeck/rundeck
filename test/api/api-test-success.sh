#!/bin/bash

# usage:
#  api-test-success.sh <file.xml> [message]
# expects success response for the file.xml, optionally expects a certain message

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
  errorMsg 'FAIL: expected json content'
  >&2 cat  ${file}
  exit 2
fi

type=$(jq -r "type" ${file})
if [ "$type" != "array" ] ; then
  waserror=$(jq -r ".error" ${file})
  if [ "true" == "$waserror" ] ; then
      errorMsg "FAIL: Server reported an error: "
      >&2 jq -r ".message"  ${file}
      exit 2
  fi
fi

exit 0
