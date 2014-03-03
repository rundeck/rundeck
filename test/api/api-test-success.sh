#!/bin/bash

# usage:
#  api-test-success.sh <file.xml> [message]
# expects success response for the file.xml, optionally expects a certain message

errorMsg() {
   echo "$*" 1>&2
}

SRC_DIR=$(cd `dirname $0` && pwd)
DIR=${TMP_DIR:-$SRC_DIR}

file="$1"
shift

message="$*"

XMLSTARLET=${XMLSTARLET:-xmlstarlet}

#test ${file} for valid xml
$XMLSTARLET val -w ${file} > /dev/null 2>&1
if [ 0 != $? ] ; then
    errorMsg "FAIL: Response was not valid xml"
    exit 2
fi

#If <result error="true"> then an error occured.
waserror=$($XMLSTARLET sel -T -t -v "/result/@error" ${file})
if [ "true" == "$waserror" ] ; then
    errorMsg "FAIL: Server reported an error: "
    $XMLSTARLET sel -T -t -m "/result/error/message" -v "." -n  ${file}
    exit 2
fi


##
# result wrapper is NOT expected
##
#
if [ -n "$API_XML_NO_WRAPPER" ] ; then
    # fail if <result> element is present
    $XMLSTARLET el ${file} | grep -e '^result' -q
    if [ 0 == $? ] ; then
        errorMsg "FAIL: Response was not expected to contain 'result' element"
        exit 2
    fi
    exit 0
fi

##
# result wrapper is expected
##

#test for expected /joblist element
$XMLSTARLET el ${file} | grep -e '^result' -q
if [ 0 != $? ] ; then
    errorMsg "FAIL: Response did not contain expected result"
    exit 2
fi

wassucc=$($XMLSTARLET sel -T -t -v "/result/@success" ${file})
if [ "true" != "$wassucc" ] ; then
    errorMsg "FAIL: Server did not report success: "
    $XMLSTARLET sel -T -t -m "/result/error/message" -v "." -n  ${file}
    exit 2
fi
if [ "" != "${message}" ] ; then 
    sucmsg=$($XMLSTARLET sel -T -t -v "/result/success/message" ${file})
    if [ "${sucmsg}" != "${message}" ] ; then
        errorMsg "FAIL: wrong success message: \"${sucmsg}\", expected \"${message}\""
        exit 2
    fi
fi

exit 0