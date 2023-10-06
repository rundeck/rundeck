#!/bin/bash

#test output from /api/jobs/export

DIR=$(cd `dirname $0` && pwd)

source $DIR/include.sh

# now submit req

proj=$2
if [ "" == "$2" ] ; then
    proj="test"
fi
runurl="${APIURL}/project/${proj}/jobs/export"

echo "TEST: export RunDeck Jobs in jobs.json format"

params="format=json"

# get listing
docurl  -D $DIR/headers.out ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#test headers.out for valid yaml content type
grep "Content-Type: application/json" $DIR/headers.out -q
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response was not json"
    exit 2
fi


# job export doesn't wrap result in common result wrapper
#Check projects list
itemcount=$(json_val ". | length" $DIR/curl.out)
if [ "" == "$itemcount" ] ; then
    errMsg "Unexpected result"
    exit 2
else
    echo "OK"
fi


echo "TEST: export RunDeck Jobs in jobs default (json) format"

params=""

# get listing
docurl  -D $DIR/headers.out ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#test headers.out for valid yaml content type
grep "Content-Type: application/json" $DIR/headers.out -q
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response was not json"
    exit 2
fi


# job export doesn't wrap result in common result wrapper
#Check projects list
itemcount=$(json_val ". | length" $DIR/curl.out)
if [ "" == "$itemcount" ] ; then
    errMsg "Unexpected result"
    exit 2
else
    echo "OK"
fi



rm $DIR/curl.out

