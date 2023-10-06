#!/bin/bash

#test output from /api/jobs

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

proj="test"
# now submit req
runurl="${APIURL}/project/$proj/jobs"


echo "Listing RunDeck Jobs for project ${proj}..."

params=""

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$(jq -r ". | length" $DIR/curl.out)

if [ "" == "$itemcount" ] ; then
    errorMsg "Wrong count: $itemcount"
    exit 2
fi
echo "OK"

###
#load two jobs with similar names
###

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
  description: 'test-jobs.sh script'
  name: test-jobs
  group: api/test-jobs
END

# now submit req
runurl="${APIURL}/project/$proj/jobs/import"

params="format=yaml&dupeOption=skip"

# specify the file for upload with curl, named "xmlBatch"
ulopts="-F xmlBatch=@$DIR/temp.out"

# get listing
docurl $ulopts  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#wrapper expected in job import response
$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

cat > $DIR/temp.out <<END
-
  project: test
  loglevel: INFO
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - exec: echo hello there
  description: 'test-jobs.sh script'
  name: test-jobs another job
  group: api/test-jobs/sub-group
END

# now submit req
runurl="${APIURL}/project/$proj/jobs/import"

params="format=yaml&dupeOption=skip"

# specify the file for upload with curl, named "xmlBatch"
ulopts="-F xmlBatch=@$DIR/temp.out"

# get listing
docurl $ulopts  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#wrapper expected in job import response
$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

# load a top-level job not in a group
cat > $DIR/temp.out <<END
-
  project: test
  loglevel: INFO
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - exec: echo hello there
  description: 'test-jobs.sh script'
  name: test-jobs top level
END

# now submit req
runurl="${APIURL}/project/$proj/jobs/import"

params="format=yaml&dupeOption=skip"

# specify the file for upload with curl, named "xmlBatch"
ulopts="-F xmlBatch=@$DIR/temp.out"

# get listing
docurl $ulopts  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#wrapper expected in job import response
$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

###
# test query with match filter and exact filter
###
testQuery(){
  local query=${1};shift
  local expect=${1:-1};shift

  runurl="${APIURL}/project/$proj/jobs"

  # get listing
  docurl ${runurl}?${query} > $DIR/curl.out
  if [ 0 != $? ] ; then
      errorMsg "ERROR: failed query request"
      exit 2
  fi

  $SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

  #Check projects list
  itemcount=$(jq -r "length" $DIR/curl.out)

  if [ "${expect}" != "$itemcount" ] ; then
      errorMsg "Wrong count: $itemcount"
      exit 2
  fi

  rm $DIR/curl.out
}

echo "Test inexact jobs query.."
testQuery "project=${proj}&jobFilter=test-jobs&groupPath=api/test-jobs" "2"
echo "OK"


echo "Test inexact jobs query, exact group.."
testQuery "project=${proj}&jobFilter=test-jobs&groupPathExact=api/test-jobs"
echo "OK"



echo "Test inexact jobs query, exact name.."
testQuery "project=${proj}&jobExactFilter=test-jobs&groupPath=api/test-jobs"
echo "OK"



echo "Test inexact jobs query, group only.."
testQuery "project=${proj}&groupPath=api/test-jobs" "2"
echo "OK"


echo "Test exact name, exact group.."
testQuery "project=${proj}&jobExactFilter=test-jobs&groupPathExact=api/test-jobs"
echo "OK"

echo "Test exact name, exact group.."
testQuery "project=${proj}&jobExactFilter=test-jobs+another+job&groupPathExact=api/test-jobs/sub-group"
echo "OK"


echo "Test exact name, exact group 2.."
testQuery "project=${proj}&jobExactFilter=test-jobs&groupPathExact=api/test-jobs"
echo "OK"

echo "Test exact name, exact group, no match.."
testQuery "project=${proj}&jobExactFilter=test-jobs+another&groupPathExact=api/test-jobs" "0"
echo "OK"


echo "Test exact name, exact group, no match 2.."
testQuery "project=${proj}&jobExactFilter=test-jobs&groupPathExact=api/test-jobs/sub-group" "0"
echo "OK"

echo "Test match name, exact group, top level.."
testQuery "project=${proj}&jobFilter=test-jobs&groupPathExact=-"
echo "OK"

