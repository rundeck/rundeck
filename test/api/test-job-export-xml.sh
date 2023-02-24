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

# now submit req
runurl="${APIURL}/job/${jobid}"

validate_xml_job(){
  local FILE=$1
  #test curl.out for valid xml
  $XMLSTARLET val -w $FILE > /dev/null 2>&1
  if [ 0 != $? ] ; then
      errorMsg "ERROR: Response was not valid xml"
      exit 2
  fi

  #test for expected /joblist element
  $XMLSTARLET el $FILE | grep -e '^joblist' -q
  if [ 0 != $? ] ; then
      errorMsg "ERROR: Response did not contain expected result"
      exit 2
  fi

  # job export doesn't wrap result in common result wrapper
  #Check projects list
  itemcount=$($XMLSTARLET sel -T -t -m "/joblist" -v "count(job)" $FILE)
  foundjobid=$($XMLSTARLET sel -T -t -m "/joblist" -v "job/uuid" $FILE)
  if [ "1" == "$itemcount" -a "$jobid" == "$foundjobid" ] ; then
      echo "OK"
  else
      fail "Wrong job count: $itemcount, or wrong found id: $foundjobid"
  fi
}

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
validate_xml_job $DIR/curl.out


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
validate_xml_job $DIR/curl.out

