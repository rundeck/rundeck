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


validate_json_file(){
  local FILE=$1
  # job export doesn't wrap result in common result wrapper
  #Check projects list
  itemcount=$(json_val  ". | length" $FILE)
  foundjobid=$(json_val  ".[0].id" $FILE)
  if [ "1" == "$itemcount" -a "$jobid" == "$foundjobid" ] ; then
      echo "OK"
  else
      fail "Wrong job count: $itemcount, or wrong found id: $foundjobid"
  fi
}


echo "TEST: export single job in jobs.json format (format param)"


# now submit req
runurl="${APIURL}/job/${jobid}"
params="format=json"

# get listing
docurl  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi
validate_json_file $DIR/curl.out


echo "TEST: export single job in jobs.json format (Accept header)"


# now submit req
runurl="${APIURL}/job/${jobid}"
params=""

# get listing
docurl -H 'Accept: application/json' ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi
validate_json_file $DIR/curl.out
