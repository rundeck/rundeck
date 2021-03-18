#!/bin/bash

#test output from /api/job/{id}

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

###
# setup: create a new job and acquire the ID
###

# job exec
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
<joblist>
   <job>
      <name>cli job</name>
      <group>api-test/job-export</group>
      <description></description>
      <loglevel>INFO</loglevel>
      <context>
          <project>$xmlproj</project>
      </context>
      <dispatch>
        <threadcount>1</threadcount>
        <keepgoing>true</keepgoing>
      </dispatch>
      <sequence>
        <command>
        <exec>$xmlargs</exec>
        </command>
      </sequence>
   </job>
</joblist>

END

jobid=$(uploadJob "$DIR/temp.out" "$project"  1 "")
if [ 0 != $? ] ; then
  errorMsg "failed job upload"
  exit 2
fi


###
# Export the chosen id
###

echo "TEST: export single job in default format (xml)"


# now submit req
runurl="${APIURL}/job/${jobid}"
params=""

# get listing
docurl  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#test curl.out for valid xml
$XMLSTARLET val -w $DIR/curl.out > /dev/null 2>&1
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response was not valid xml"
    exit 2
fi

#test for expected /joblist element
$XMLSTARLET el $DIR/curl.out | grep -e '^joblist' -q
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response did not contain expected result"
    exit 2
fi

# job export doesn't wrap result in common result wrapper
#Check projects list
itemcount=$($XMLSTARLET sel -T -t -m "/joblist" -v "count(job)" $DIR/curl.out)
foundjobid=$($XMLSTARLET sel -T -t -m "/joblist" -v "job/uuid" $DIR/curl.out)
if [ "1" == "$itemcount" -a "$jobid" == "$foundjobid" ] ; then
    echo "OK"
else
    fail "Wrong job count: $itemcount, or wrong found id: $foundjobid"
fi

echo "TEST: export single job in jobs.xml format (format param)"


# now submit req
runurl="${APIURL}/job/${jobid}"
params="format=xml"

# get listing
docurl  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#test curl.out for valid xml
$XMLSTARLET val -w $DIR/curl.out > /dev/null 2>&1
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response was not valid xml"
    exit 2
fi

#test for expected /joblist element
$XMLSTARLET el $DIR/curl.out | grep -e '^joblist' -q
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response did not contain expected result"
    exit 2
fi

# job export doesn't wrap result in common result wrapper
#Check projects list
itemcount=$($XMLSTARLET sel -T -t -m "/joblist" -v "count(job)" $DIR/curl.out)
foundjobid=$($XMLSTARLET sel -T -t -m "/joblist" -v "job/uuid" $DIR/curl.out)
if [ "1" == "$itemcount" -a "$jobid" == "$foundjobid" ] ; then
    echo "OK"
else
    fail "Wrong job count: $itemcount, or wrong found id: $foundjobid"
fi


echo "TEST: export single job in jobs.xml format (Accept header)"


# now submit req
runurl="${APIURL}/job/${jobid}"
params=""

# get listing
docurl -H 'Accept: application/xml' ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

#test curl.out for valid xml
$XMLSTARLET val -w $DIR/curl.out > /dev/null 2>&1
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response was not valid xml"
    exit 2
fi

#test for expected /joblist element
$XMLSTARLET el $DIR/curl.out | grep -e '^joblist' -q
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response did not contain expected result"
    exit 2
fi

# job export doesn't wrap result in common result wrapper
#Check projects list
itemcount=$($XMLSTARLET sel -T -t -m "/joblist" -v "count(job)" $DIR/curl.out)
foundjobid=$($XMLSTARLET sel -T -t -m "/joblist" -v "job/uuid" $DIR/curl.out)
if [ "1" == "$itemcount" -a "$jobid" == "$foundjobid" ] ; then
    echo "OK"
else
    fail "Wrong job count: $itemcount, or wrong found id: $foundjobid"
fi


###
# Export the chosen id, with format=yaml
###

echo "TEST: export single job in jobs.yaml format (format param)"


# now submit req
runurl="${APIURL}/job/${jobid}"
params="format=yaml"

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
grep "id: ${jobid}" $DIR/curl.out -q
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response yaml did not have expected job id"
    exit 2
fi
echo OK

rm $DIR/curl.out

###
# Export the chosen id, with Accept: application/yaml
###

echo "TEST: export single job in jobs.yaml format (Accept header)"


# now submit req
runurl="${APIURL}/job/${jobid}"
params=""

# get listing
docurl -H 'Accept: application/yaml' -D $DIR/headers.out ${runurl}?${params} > $DIR/curl.out
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
grep "id: ${jobid}" $DIR/curl.out -q
if [ 0 != $? ] ; then
    errorMsg "ERROR: Response yaml did not have expected job id"
    exit 2
fi
echo OK

rm $DIR/curl.out




###
# Export the chosen id, with unsupported content type application/json
###

echo "TEST: export single job in unsupported format (Accept header)"


# now submit req
runurl="${APIURL}/job/${jobid}"
params="format=json"

# get listing
# specify the file for upload with curl, named "xmlBatch"
ulopts="-H Accept:application/json"

CURL_REQ_OPTS=$ulopts $SHELL $SRC_DIR/api-expect-error.sh "${runurl}" "${params}" "The format is not valid: json" 415 || exit 2
echo OK
rm $DIR/curl.out
###
# Export the chosen id, with unsupported content type application/json
###

echo "TEST: export single job in unsupported format (format param)"


# now submit req
runurl="${APIURL}/job/${jobid}"
params="format=json"

# get listing
$SHELL $SRC_DIR/api-expect-error.sh "${runurl}" "${params}" "The format is not valid: json" 415 || exit 2
echo OK
rm $DIR/curl.out
