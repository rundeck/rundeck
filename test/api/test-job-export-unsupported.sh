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
# Export the chosen id, with unsupported content type application/json
###

echo "TEST: export single job in unsupported format (Accept header)"


# now submit req
runurl="${APIURL}/job/${jobid}"
params=""


ulopts="-H Accept:text/csv"

CURL_REQ_OPTS=$ulopts $SHELL $SRC_DIR/api-expect-error.sh "${runurl}" "" "The format is not valid: csv" 415 || exit 2
echo OK
rm $DIR/curl.out
###
# Export the chosen id, with unsupported content type application/json
###

echo "TEST: export single job in unsupported format (format param)"


# now submit req
runurl="${APIURL}/job/${jobid}"
params="format=csv"

# get listing
$SHELL $SRC_DIR/api-expect-error.sh "${runurl}" "${params}" "The format is not valid: csv" 415 || exit 2
echo OK
rm $DIR/curl.out
