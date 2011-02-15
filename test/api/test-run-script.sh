#!/bin/bash

# TEST: /api/run/script action

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

proj="test"

# now submit req
runurl="${APIURL}/run/script"

echo "TEST: /api/run/script GET should fail with wrong HTTP method"
sh $DIR/api-expect-code.sh 405 "${runurl}" "project=" 'parameter "project" is required' && echo "OK" || exit 2

echo "TEST: /api/run/script POST should fail with no project param"
CURL_REQ_OPTS="-X POST" sh $DIR/api-expect-error.sh "${runurl}" "project=" 'parameter "project" is required' && echo "OK" || exit 2

echo "TEST: /api/run/script POST should fail with no scriptFile param"
params="project=${proj}"
CURL_REQ_OPTS="-X POST" sh $DIR/api-expect-error.sh "${runurl}" "${params}" 'parameter "scriptFile" is required' && echo "OK" || exit 2

echo "TEST: /api/run/script POST should fail with empty scriptFile content"
touch $DIR/test.tmp
$CURL --header "$VERSHEADER" -F scriptFile=@$DIR/test.tmp ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "FAIL: failed query request"
    exit 2
fi

sh $DIR/api-test-error.sh $DIR/curl.out "Input script file was empty" && echo "OK" || exit 2

####
#  echo a script into a temp file
####
cat <<END > $DIR/script.tmp
#!/bin/bash
echo hello
echo @node.name@ > $DIR/script.out
END

#remove script.out if it exists

[ -f $DIR/script.out ] && rm $DIR/script.out

echo "TEST: /api/run/script should succeed and return execution id"
# make api request
$CURL --header "$VERSHEADER" -F scriptFile=@$DIR/script.tmp ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "FAIL: failed query request"
    exit 2
fi

sh $DIR/api-test-success.sh $DIR/curl.out || exit 2

execid=$($XMLSTARLET sel -T -t -o "Execution started with ID: " -v "/result/execution/@id" -n $DIR/curl.out)
if [ "" == "${execid}" ] ; then
    errorMsg "FAIL: expected execution id in result: ${execid}"
    exit 2
fi

##wait for script to execute...
sleep 3

if [ ! -f $DIR/script.out ] ; then
    errorMsg "FAIL: Expected script to execute and create script.out file"
    exit 2
else
    cat $DIR/script.out
    rm $DIR/script.out
fi


echo "OK"

rm $DIR/curl.out