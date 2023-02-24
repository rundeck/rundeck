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

validate_yaml_job(){
  local FILE=$1
  local HEADERS=$2
  #test headers.out for valid yaml content type
  grep "Content-Type: text/yaml" $HEADERS -q
  if [ 0 != $? ] ; then
      errorMsg "ERROR: Response was not yaml"
      exit 2
  fi

  #test yaml output for at least the id: entry
  grep "id: ${jobid}" $FILE -q
  if [ 0 != $? ] ; then
      errorMsg "ERROR: Response yaml did not have expected job id"
      exit 2
  fi
  echo OK
}
validate_yaml_job $DIR/curl.out $DIR/headers.out

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

validate_yaml_job $DIR/curl.out $DIR/headers.out

rm $DIR/curl.out


