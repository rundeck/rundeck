#!/bin/bash

# TEST: /api/run/script action

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

proj="test"

# now submit req
runurl="${APIURL}/run/script"

####
#  echo a script into a temp file
####
cat <<END > $DIR/script.tmp
#!/bin/bash
echo \$1 > $DIR/script.out
END

#remove script.out if it exists

[ -f $DIR/script.out ] && rm $DIR/script.out

echo "TEST: /api/run/script with scriptInterpreter and interpreterArgsQuoted=true"
params="project=${proj}&scriptInterpreter=bash+-c&argString=%24%7Bnode.name%7D&interpreterArgsQuoted=true"
# make api request
docurl -F scriptFile=@$DIR/script.tmp ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "FAIL: failed query request"
    exit 2
fi

sh $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

execid=$($XMLSTARLET sel -T -t -v "/result/execution/@id" -n $DIR/curl.out)
if [ "" == "${execid}" ] ; then
    errorMsg "FAIL: expected execution id in result: ${execid}"
    exit 2
fi

##wait for script to execute...
rd-queue follow -q -e $execid || fail "Waiting for $execid to finish"
sh $SRC_DIR/api-expect-exec-success.sh $execid || exit 2

if [ ! -f $DIR/script.out ] ; then
    errorMsg "FAIL: Expected script to execute and create script.out file"
    exit 2
fi

output=$(cat $DIR/script.out)

if [ -z "$output" ] ; then
    errorMsg "FAIL: Expected script output to have node name in $DIR/script.out"
    exit 2
fi

cat $DIR/script.out
rm $DIR/script.out


echo "TEST: /api/run/script with scriptInterpreter and interpreterArgsQuoted=false"
params="project=${proj}&scriptInterpreter=bash+-c&argString=%24%7Bnode.name%7D&interpreterArgsQuoted=false"
# make api request
docurl -F scriptFile=@$DIR/script.tmp ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "FAIL: failed query request"
    exit 2
fi

sh $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

execid=$($XMLSTARLET sel -T -t -v "/result/execution/@id" -n $DIR/curl.out)
if [ "" == "${execid}" ] ; then
    errorMsg "FAIL: expected execution id in result: ${execid}"
    exit 2
fi

##wait for script to execute...
rd-queue follow -q -e $execid || fail "Waiting for $execid to finish"
sh $SRC_DIR/api-expect-exec-success.sh $execid || exit 2

if [ ! -f $DIR/script.out ] ; then
    errorMsg "FAIL: Expected script to execute and create script.out file"
    exit 2
fi

output=$(cat $DIR/script.out)

if [ ! -z "$output" ] ; then
    errorMsg "FAIL: Expected script output to be empty in $DIR/script.out"
    exit 2
fi



echo "OK"

rm $DIR/curl.out
rm $DIR/script.tmp