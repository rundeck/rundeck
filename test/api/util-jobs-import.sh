#!/bin/bash

# Usage: util-jobs-import.sh <url> <file> [format]

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

infile=$1
shift
informat=${1:-xml}


# now submit req
runurl="${APIURL}/jobs/import"

echo "Import jobs in ${informat} format"

params="dupeOption=update"

# specify the file for upload with curl, named "xmlBatch"
ulopts="-F xmlBatch=@$infile"

# get listing
docurl $ulopts ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#result will contain list of failed and succeeded jobs, in this
#case there should only be 1 failed or 1 succeeded since we submit only 1

failedcount=$(xmlsel "/result/failed/@count" $DIR/curl.out)
succount=$(xmlsel "/result/succeeded/@count" $DIR/curl.out)
skipcount=$(xmlsel "/result/skipped/@count" $DIR/curl.out)

if [ "0" != "$failedcount" ] ; then
    echo "$failedcount Failed:"
    $XMLSTARLET sel -T -t -m "/result/failed/job" -o "[" -v "id" -o "] " -v "name" -o ", " -v "group" -o ", " -v "project" -n \
    -o "ERROR: " -v "error" -n $DIR/curl.out
fi

if [ "0" != "$succount" ] ; then
    echo "$succount Succeeded:"
    $XMLSTARLET sel -T -t -m "/result/succeeded/job" -o "[" -v "id" -o "] " -v "name" -o ", " -v "group" -o ", " -v "project" -n $DIR/curl.out
fi

if [ "0" != "$skipcount" ] ; then
    echo "$skipcount Skipped:"
    $XMLSTARLET sel -T -t -m "/result/skipped/job" -o "[" -v "id" -o "] " -v "name" -o ", " -v "group" -o ", " -v "project" -n $DIR/curl.out
fi
rm $DIR/curl.out

