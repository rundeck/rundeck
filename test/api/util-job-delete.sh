#!/bin/bash


#Usage:
#   util-job-delete.sh <URL> <id>
#   Deletes specified job ID.
#
if [ $# -lt 2 ] ; then
    echo "Usage: util-jobs.sh <URL> [id..]"
    exit 2
fi

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

###
# DELETE the chosen id, expect success message
###

joblist=$@

for jobid in $joblist ; do 

    echo "Deleting job ID ${jobid}..."


    # now submit req
    runurl="${APIURL}/job/${jobid}"
    params=""

    # delete
    docurl -X DELETE ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

    $SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

    xmlsel "/result/success/message" -n $DIR/curl.out
done
    
rm $DIR/curl.out

