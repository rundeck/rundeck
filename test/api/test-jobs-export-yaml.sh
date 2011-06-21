#!/bin/bash

#test output from /api/jobs/export using format=yaml

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

# now submit req
runurl="${APIURL}/jobs/export"
proj=$2
if [ "" == "$2" ] ; then
    proj="test"
fi

echo "TEST: export RunDeck Jobs in jobs.yaml format"

params="project=${proj}&format=yaml"

# get listing
docurl -D $DIR/headers.out ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#test headers.out for valid yaml content type
grep "Content-Type: text/yaml" $DIR/headers.out -q
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response was not yaml"
    exit 2
fi

#test yaml output for at least the id: entry
grep "id:" $DIR/curl.out -q
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response yaml did not have expected content"
    exit 2
fi
echo OK


rm $DIR/curl.out
rm $DIR/headers.out


