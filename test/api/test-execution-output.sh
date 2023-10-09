#!/bin/bash

#test output from /api/execution/{id}/output

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

####
# Setup: create simple adhoc command execution to provide execution ID.
####


proj="test"
runurl="${APIURL}/project/${proj}/run/command"
params="exec=echo+testing+execution+output+api1+line+1;sleep+2;echo+line+2;sleep+2;echo+line+3;sleep+2;echo+line+4+final"

# get listing
docurl -X POST ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#select id
assert_json_not_null ".execution.id" $DIR/curl.out

execid=$(jq -r ".execution.id" < $DIR/curl.out)

if [ -z "$execid" ] ; then
    errorMsg "FAIL: expected execution id"
    exit 2
fi


#function to verify api output entry has content
verify_entry_output(){
    file=$1
    ocount=$(jq -r  ".entries|length" < $file)

    assert_json_not_null ".offset" $DIR/curl.out
    assert_json_not_null ".completed"  $DIR/curl.out
    #output text
    unmod=$(jq -r ".unmodified" < $DIR/curl.out)
    if [[ $ocount -gt 0 && $unmod != "true" ]]; then
        xout=$(jq -r ".entries[0].log" < $file)
        echo "OUT: $xout"
        if [ -z "$xout" ]; then
            errorMsg "ERROR: no output in content"
            exit 2
        fi
    fi
}

####
# Test: receive output using lastmod param
####

# now submit req
runurl="${APIURL}/execution/${execid}/output"

echo "TEST: /api/execution/${execid}/output using lastmod ..."

doff=0
ddone="false"
dlast=0
dmax=20
dc=0
dlasttemp=0
while [[ $ddone == "false" && $dc -lt $dmax ]]; do
    #statements
    params="offset=$doff&lastmod=$dlast"

    # get listing
    docurl ${runurl}?${params} > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed query request"
        exit 2
    fi

    verify_entry_output $DIR/curl.out

    unmod=$(jq -r ".unmodified" < $DIR/curl.out)
    doff=$(jq -r ".offset" < $DIR/curl.out)
    dlasttemp=$(jq -r ".lastModified" < $DIR/curl.out)
    if [ "$dlasttemp" != "null" ] && [ "$dlasttemp" != "" ]; then
        dlast=$dlasttemp
    fi
    ddone=$(jq -r ".completed" < $DIR/curl.out)
    #echo "unmod $unmod, doff $doff, dlast $dlast, ddone $ddone"
    if [[ $unmod == "true" ]]; then
        #echo "unmodifed, sleep 3..."
        sleep 2
    else

        #echo "$ocount lines, sleep 1"
        if [[ $ddone != "true" ]]; then
            sleep 1
        fi
    fi
    dc=$(( $dc + 1 ))

done

if [[ $ddone != "true" ]]; then
    errorMsg "ERROR: not all output was received in $dc requests"
    exit 2
fi

echo "OK"

##wait for exec to finish...
api_waitfor_execution $execid || fail "Waiting for $execid to finish"
$SHELL $SRC_DIR/api-expect-exec-success.sh $execid || exit 2



####
# Setup: run adhoc command to output lines
####


proj="test"
runurl="${APIURL}/project/${proj}/run/command"
params="exec=echo+testing+execution+output+api2+line+1;sleep+2;echo+line+2;sleep+2;echo+line+3;sleep+2;echo+line+4+final"

# get listing
docurl -X POST ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#select id
execid=$(jq -r ".execution.id" < $DIR/curl.out)

if [ -z "$execid" ] ; then
    errorMsg "FAIL: expected execution id"
    exit 2
fi


####
# Test: receive output with maxlines param
####

# now submit req
runurl="${APIURL}/execution/${execid}/output"

echo "TEST: /api/execution/${execid}/output using maxlines..."

doff=0
ddone="false"
dlast=0
dmax=20
dc=0
while [[ $ddone == "false" && $dc -lt $dmax ]]; do
    #statements
    params="offset=$doff&maxlines=1"

    # get listing
    docurl ${runurl}?${params} > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed query request"
        exit 2
    fi

    verify_entry_output $DIR/curl.out

    unmod=$(jq -r ".unmodified" < $DIR/curl.out)
    doff=$(jq -r ".offset" < $DIR/curl.out)
    dlast=$(jq -r ".lastModified" < $DIR/curl.out)
    ddone=$(jq -r ".completed" < $DIR/curl.out)
    #echo "unmod $unmod, doff $doff, dlast $dlast, ddone $ddone"
    if [[ $unmod == "true" ]]; then
        #echo "unmodifed, sleep 3..."
        sleep 2
    else
        #echo "$ocount lines, sleep 1"
        if [[ $ddone != "true" ]]; then
            sleep 1
        fi
    fi
    dc=$(( $dc + 1 ))

done

if [[ $ddone != "true" ]]; then
    errorMsg "ERROR: not all output was received in $dc requests"
    exit 2
fi

echo "OK"

##wait for exec to finish...
api_waitfor_execution $execid || fail "Waiting for $execid to finish"
$SHELL $SRC_DIR/api-expect-exec-success.sh $execid || exit 2


####
# Setup: run adhoc command to output lines
####


proj="test"
runurl="${APIURL}/project/${proj}/run/command"
params="exec=echo+testing+execution+output+api3+line+1;sleep+2;echo+line+2;sleep+2;echo+line+3;sleep+2;echo+line+4+final"

# get listing
docurl -X POST ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#select id
execid=$(jq -r ".execution.id" < $DIR/curl.out)

if [ -z "$execid" ] ; then
    errorMsg "FAIL: expected execution id"
    exit 2
fi




####
# Test: specify json format by default
####

# now submit req
runurl="${APIURL}/execution/${execid}/output"

echo "TEST: /api/execution/${execid}/output ..."

doff=0
ddone="false"
dlast=0
dmax=20
dc=0
while [[ $ddone == "false" && $dc -lt $dmax ]]; do
    #statements
    params="offset=$doff"

    # get listing
    docurl ${runurl}?${params} > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed query request"
        exit 2
    fi

    verify_entry_output $DIR/curl.out

    unmod=$(jq -r ".unmodified" < $DIR/curl.out)
    doff=$(jq -r ".offset" < $DIR/curl.out)
    dlast=$(jq -r ".lastModified" < $DIR/curl.out)
    ddone=$(jq -r ".completed" < $DIR/curl.out)
    #echo "unmod $unmod, doff $doff, dlast $dlast, ddone $ddone"
    if [[ $unmod == "true" ]]; then

        #echo "unmodifed, sleep 3..."
        sleep 2
    else
        #echo "$ocount lines, sleep 1"
        if [[ $ddone != "true" ]]; then
            sleep 1
        fi
    fi
    dc=$(( $dc + 1 ))

done

if [[ $ddone != "true" ]]; then
    errorMsg "ERROR: not all output was received in $dc requests"
    exit 2
fi


##wait for exec to finish...
api_waitfor_execution $execid || fail "Waiting for $execid to finish"
$SHELL $SRC_DIR/api-expect-exec-success.sh $execid || exit 2

echo "OK"
