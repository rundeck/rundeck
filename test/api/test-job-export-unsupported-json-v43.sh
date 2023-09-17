#!/bin/bash

#test job export json format is unsupported in api <v44

DIR=$(cd `dirname $0` && pwd)
API_VERSION=43
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
      <name>trst-job-export-unsupported-json-v43</name>
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


ulopts="-H Accept:application/json"

CURL_REQ_OPTS=$ulopts $SHELL $SRC_DIR/api-expect-error.sh "${runurl}" "" "The format is not valid: json" 415 || exit 2
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
