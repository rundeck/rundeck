#!/bin/bash

#test output from /api/execution/{id}/output

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

####
# Setup: create simple adhoc command execution to provide execution ID.
####

runurl="${APIURL}/run/command"
proj="test"
params="project=${proj}&exec=echo+testing+execution+output+api-plain+line+1;echo+line+2;echo+line+3;echo+line+4+final"

expectfile1=$DIR/expect-exec-output-plain1.txt
expectfile2=$DIR/expect-exec-output-plain2.txt
expectfile3=$DIR/expect-exec-output-plain3.txt
expectfile4=$DIR/expect-exec-output-plain4.txt

cat > $expectfile4 <<END
testing execution output api-plain line 1
line 2
line 3
line 4 final
END
tail -n 3 $expectfile4 > $expectfile3
tail -n 2 $expectfile4 > $expectfile2
tail -n 1 $expectfile4 > $expectfile1

# get listing
docurl -X POST ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#select id

execid=$($XMLSTARLET sel -T -t -v "/result/execution/@id" $DIR/curl.out)

if [ -z "$execid" ] ; then
    errorMsg "FAIL: expected execution id"
    exit 2
fi


##wait for exec to finish...
rd-queue follow -q -e $execid || fail "Waiting for $execid to finish"
$SHELL $SRC_DIR/api-expect-exec-success.sh $execid || exit 2


####
# Test: receive last lines 4
####
for lines in $(seq 4) ; do
    # now submit req
    runurl="${APIURL}/execution/${execid}/output.text"
    params="lastlines=$lines"

    echo "TEST: ${runurl} using lastlines=$lines ..."

    outfile=$DIR/test-exec-output-plain.txt

        # get listing
    docurl -D $DIR/headers.out ${runurl}?${params} > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed query request"
        exit 2
    fi
    grep "HTTP/1.1 200" -q $DIR/headers.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed query request"
        exit 2
    fi

    ocount=$(wc -l $DIR/curl.out)

    # if [[ $ocount != $lines ]]; then
    #     errorMsg "Expected to see ${lines} lines, saw $ocount in $DIR/curl.out"
    #     exit 2
    # fi

    diff -q $DIR/expect-exec-output-plain${lines}.txt $DIR/curl.out
    if [[ 0 != $? ]] ; then
        errorMsg "ERROR: output was not expected"
        diff $DIR/expect-exec-output-plain${lines}.txt $DIR/curl.out
        exit 2
    fi

    #output text



done

echo "OK"
