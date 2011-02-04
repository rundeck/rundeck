#!/bin/bash

# usage:
#  api-expect-success.sh <file.xml>
# expects success response for the file.xml

errorMsg() {
   echo "$*" 1>&2
}

DIR=$(cd `dirname $0` && pwd)

file="$1"
shift

XMLSTARLET=xml

#test ${file} for valid xml
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
if [ "true" == "$waserror" ] ; then
    errorMsg "FAIL: Server reported an error: "
    $XMLSTARLET sel -T -t -m "/result/error/message" -v "." -n  ${file}
    exit 2
fi
wassucc=$($XMLSTARLET sel -T -t -v "/result/@success" ${file})
if [ "true" != "$wassucc" ] ; then
    errorMsg "FAIL: Server did not report success: "
    $XMLSTARLET sel -T -t -m "/result/error/message" -v "." -n  ${file}
    exit 2
fi

exit 0