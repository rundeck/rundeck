#!/bin/bash

#test output from /api/jobs

DIR=$(cd `dirname $0` && pwd)
API_VERSION=13
source $DIR/include.sh

# now submit req
runurl="${APIURL}/jobs"

proj="test"

echo "Listing RunDeck Jobs for project ${proj}..."

params="project=${proj}"

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/jobs/@count" $DIR/curl.out)

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
runurl="${APIURL}/jobs/import"

params="format=yaml&dupeOption=skip"

# specify the file for upload with curl, named "xmlBatch"
ulopts="-F xmlBatch=@$DIR/temp.out"

# get listing
docurl $ulopts  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

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
runurl="${APIURL}/jobs/import"

params="format=yaml&dupeOption=skip"

# specify the file for upload with curl, named "xmlBatch"
ulopts="-F xmlBatch=@$DIR/temp.out"

# get listing
docurl $ulopts  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

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
runurl="${APIURL}/jobs/import"

params="format=yaml&dupeOption=skip"

# specify the file for upload with curl, named "xmlBatch"
ulopts="-F xmlBatch=@$DIR/temp.out"

# get listing
docurl $ulopts  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

###
# test query with match filter and exact filter
###

echo "Test inexact jobs query.."

runurl="${APIURL}/jobs"

params="project=${proj}&jobFilter=test-jobs&groupPath=api/test-jobs"

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/jobs/@count" $DIR/curl.out)

if [ "2" != "$itemcount" ] ; then
    errorMsg "Wrong count: $itemcount"
    exit 2
fi
echo "OK"

rm $DIR/curl.out


echo "Test inexact jobs query, exact group.."

runurl="${APIURL}/jobs"

params="project=${proj}&jobFilter=test-jobs&groupPathExact=api/test-jobs"

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/jobs/@count" $DIR/curl.out)

if [ "1" != "$itemcount" ] ; then
    errorMsg "Wrong count: $itemcount"
    exit 2
fi
echo "OK"

rm $DIR/curl.out


echo "Test inexact jobs query, exact name.."

runurl="${APIURL}/jobs"

params="project=${proj}&jobExactFilter=test-jobs&groupPath=api/test-jobs"

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/jobs/@count" $DIR/curl.out)

if [ "1" != "$itemcount" ] ; then
    errorMsg "Wrong count: $itemcount"
    exit 2
fi
echo "OK"

rm $DIR/curl.out


echo "Test inexact jobs query, group only.."

runurl="${APIURL}/jobs"

params="project=${proj}&groupPath=api/test-jobs"

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/jobs/@count" $DIR/curl.out)

if [ "2" != "$itemcount" ] ; then
    errorMsg "Wrong count: $itemcount"
    exit 2
fi
echo "OK"

rm $DIR/curl.out

echo "Test exact name, exact group.."

runurl="${APIURL}/jobs"

params="project=${proj}&jobExactFilter=test-jobs&groupPathExact=api/test-jobs"

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/jobs/@count" $DIR/curl.out)

if [ "1" != "$itemcount" ] ; then
    errorMsg "Wrong count: $itemcount"
    exit 2
fi
echo "OK"

rm $DIR/curl.out

echo "Test exact name, exact group.."

runurl="${APIURL}/jobs"

params="project=${proj}&jobExactFilter=test-jobs+another+job&groupPathExact=api/test-jobs/sub-group"

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/jobs/@count" $DIR/curl.out)

if [ "1" != "$itemcount" ] ; then
    errorMsg "Wrong count: $itemcount"
    exit 2
fi
echo "OK"

rm $DIR/curl.out


echo "Test exact name, exact group 2.."

runurl="${APIURL}/jobs"

params="project=${proj}&jobExactFilter=test-jobs&groupPathExact=api/test-jobs"

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/jobs/@count" $DIR/curl.out)

if [ "1" != "$itemcount" ] ; then
    errorMsg "Wrong count: $itemcount"
    exit 2
fi
echo "OK"

rm $DIR/curl.out

echo "Test exact name, exact group, no match.."

runurl="${APIURL}/jobs"

params="project=${proj}&jobExactFilter=test-jobs+another&groupPathExact=api/test-jobs"

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/jobs/@count" $DIR/curl.out)

if [ "0" != "$itemcount" ] ; then
    errorMsg "Wrong count: $itemcount"
    exit 2
fi
echo "OK"

rm $DIR/curl.out

echo "Test exact name, exact group, no match 2.."

runurl="${APIURL}/jobs"

params="project=${proj}&jobExactFilter=test-jobs&groupPathExact=api/test-jobs/sub-group"

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/jobs/@count" $DIR/curl.out)

if [ "0" != "$itemcount" ] ; then
    errorMsg "Wrong count: $itemcount"
    exit 2
fi
echo "OK"

rm $DIR/curl.out

echo "Test match name, exact group, top level.."

runurl="${APIURL}/jobs"

params="project=${proj}&jobFilter=test-jobs&groupPathExact=-"

# get listing
docurl ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

#Check projects list
itemcount=$($XMLSTARLET sel -T -t -v "/result/jobs/@count" $DIR/curl.out)

if [ "1" != "$itemcount" ] ; then
    errorMsg "Wrong count: $itemcount"
    exit 2
fi
echo "OK"

rm $DIR/curl.out