#!/bin/bash

#test /api/jobs/import using yaml formatted content

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

args="echo hello there"

project=$2
if [ "" == "$2" ] ; then
    project="test"
fi

#escape the string for xml
xmlargs=$($XMLSTARLET esc "$args")
xmlproj=$($XMLSTARLET esc "$project")

#produce job.xml content corresponding to the dispatch request
cat > $DIR/temp.out <<END
- 
  project: test
  loglevel: INFO
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - exec: echo hello there
  description: ''
  name: cli job2
END

# now submit req
runurl="${APIURL}/project/$project/jobs/import"

echo "TEST: import RunDeck Jobs in yaml format (multipart file)"

params="format=yaml"

# specify the file for upload with curl, named "xmlBatch"
ulopts="-F xmlBatch=@$DIR/temp.out"

# get listing
docurl $ulopts  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#result will contain list of failed and succeeded jobs, in this
#case there should only be 1 failed or 1 succeeded since we submit only 1

failedcount=$($XMLSTARLET sel -T -t -v "/result/failed/@count" $DIR/curl.out)
succount=$($XMLSTARLET sel -T -t -v "/result/succeeded/@count" $DIR/curl.out)
skipcount=$($XMLSTARLET sel -T -t -v "/result/skipped/@count" $DIR/curl.out)

if [ "1" != "$succount" ] ; then
    errorMsg  "Upload was not successful."
    echo $($XMLSTARLET sel -T -t -m "/result/failed" -v "job/error" $DIR/curl.out)
    exit 2
else
    echo "OK"
fi

#test form data instead of multipart

echo "TEST: import RunDeck Jobs in yaml format (urlencode)"

params="format=yaml"

# specify the file for upload with curl, named "xmlBatch"
ulopts="--data-urlencode xmlBatch@$DIR/temp.out"

# get listing
docurl $ulopts  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#result will contain list of failed and succeeded jobs, in this
#case there should only be 1 failed or 1 succeeded since we submit only 1

failedcount=$($XMLSTARLET sel -T -t -v "/result/failed/@count" $DIR/curl.out)
succount=$($XMLSTARLET sel -T -t -v "/result/succeeded/@count" $DIR/curl.out)
skipcount=$($XMLSTARLET sel -T -t -v "/result/skipped/@count" $DIR/curl.out)

if [ "1" != "$succount" ] ; then
    errorMsg  "Upload was not successful."
    exit 2
else
    echo "OK"
fi


#rm $DIR/curl.out
rm $DIR/temp.out

