#!/bin/bash

# TEST: /api/run/script action

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

proj="test"

# now submit req
runurl="${APIURL}/run/script"

echo "TEST: /api/run/script GET should fail with wrong HTTP method"
$SHELL $SRC_DIR/api-expect-code.sh 405 "${runurl}" "project=" 'parameter "project" is required' && echo "OK" || exit 2

echo "TEST: /api/run/script POST should fail with no project param"
CURL_REQ_OPTS="-X POST" $SHELL $SRC_DIR/api-expect-error.sh "${runurl}" "project=" 'parameter "project" is required' && echo "OK" || exit 2

echo "TEST: /api/run/script POST should fail with no scriptFile param"
params="project=${proj}"
CURL_REQ_OPTS="-X POST" $SHELL $SRC_DIR/api-expect-error.sh "${runurl}" "${params}" 'parameter "scriptFile" is required' && echo "OK" || exit 2

echo "TEST: /api/run/script POST should fail with empty scriptFile content"
touch $DIR/test.tmp
docurl -F scriptFile=@$DIR/test.tmp ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "FAIL: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-error.sh $DIR/curl.out "Input script file was empty" && echo "OK" || exit 2
rm $DIR/test.tmp

OUTF=/tmp/script.out
SCRIPTF=/tmp/script.tmp
####
#  echo a script into a temp file
####
cat <<END > $SCRIPTF
#!/bin/bash
echo hello
echo @node.name@ > $OUTF
END

#remove script.out if it exists

[ -f $OUTF ] && rm $OUTF

echo "TEST: /api/run/script should succeed and return execution id"
# make api request
docurl -F scriptFile=@$SCRIPTF ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "FAIL: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

execid=$($XMLSTARLET sel -T -t -v "/result/execution/@id" -n $DIR/curl.out)
if [ "" == "${execid}" ] ; then
    errorMsg "FAIL: expected execution id in result: ${execid}"
    exit 2
fi

##wait for script to execute...
rd-queue follow -q -e $execid || fail "Waiting for $execid to finish"
$SHELL $SRC_DIR/api-expect-exec-success.sh $execid || exit 2

if [ ! -f $OUTF ] ; then
    errorMsg "FAIL: Expected script to execute and create script.out file"
    exit 2
else
    cat $OUTF
    rm $OUTF
fi


echo "OK"

rm $DIR/curl.out
rm $SCRIPTF