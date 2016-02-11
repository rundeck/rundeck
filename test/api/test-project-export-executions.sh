#!/bin/bash


#export API_XML_NO_WRAPPER=true

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

set -euo pipefail

#set -x

create_project(){
  local test_proj="$1"
  # now submit req
  local runurl="${APIURL}/projects"
  local params=""

  
  
  cat > $DIR/proj_create.post <<END
  <project>
      <name>$test_proj</name>
      <description>test1</description>
      
  </project>
END

  # post
  docurl -X POST -D $DIR/headers.out --data-binary @$DIR/proj_create.post -H Content-Type:application/xml ${runurl}?${params} > $DIR/curl.out
  if [ 0 != $? ] ; then
      errorMsg "ERROR: failed POST request"
      exit 2
  fi
  assert_http_status 201 $DIR/headers.out
}

delete_project(){
  local test_proj=$1;shift

  # now delete the test project

  runurl="${APIURL}/project/$test_proj"
  docurl -X DELETE  ${runurl} > $DIR/curl.out
  if [ 0 != $? ] ; then
      errorMsg "ERROR: failed DELETE request"
      exit 2
  fi

}


uploadJob(){
    local test_proj=$1;shift
    local file=$1;shift

    # now submit req
    runurl="${APIURL}/project/$test_proj/jobs/import"

    params="dupeOption=update"

    # specify the file for upload with curl, named "xmlBatch"
    ulopts="-F xmlBatch=@$file"

    # get listing
    docurl $ulopts  ${runurl}?${params} > $DIR/curl.out
    if [ 0 != $? ] ; then
        errorMsg "ERROR: failed query request"
        exit 2
    fi

    $SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

    #result will contain list of failed and succeeded jobs, in this
    #case there should only be 1 failed or 1 succeeded since we submit only 1

    succount=$($XMLSTARLET sel -T -t -v "/result/succeeded/@count" $DIR/curl.out)
    jobid=$($XMLSTARLET sel -T -t -v "/result/succeeded/job/id" $DIR/curl.out)

    if [ "1" != "$succount" -o "" == "$jobid" ] ; then
        $XMLSTARLET sel -T -t -v "//failed" $DIR/curl.out 1>&2
        fail  "Expected 1 successful job upload, but it was not successful." 
    fi

    echo $jobid
}


runJob(){
    local jobid=$1;shift
    # now run the job
    runurl="${APIURL}/job/${jobid}/run"
    params=""
    execargs="-opt2 a"

    # get listing
    docurl --data-urlencode "argString=${execargs}" ${runurl}?${params} > $DIR/curl.out || fail "failed request: ${runurl}"

    $SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

    #get execid

    execcount=$($XMLSTARLET sel -T -t -v "/result/executions/@count" $DIR/curl.out)
    execid=$($XMLSTARLET sel -T -t -v "/result/executions/execution/@id" $DIR/curl.out)

    if [ "1" != "${execcount}" -o "" == "${execid}" ] ; then
        errorMsg "FAIL: expected run success message for execution id. (count: ${execcount}, id: ${execid})"
        exit 2
    fi
    echo $execid
}

get_archive(){
  local test_proj=$1;shift
  local archive_file=$1;shift
  local PARAMS=$1;shift
  runurl="${APIURL}/project/$test_proj/export"

  
  ENDPOINT="${APIURL}/project/$test_proj/export"
  

  docurl -D $DIR/headers.out -o $archive_file  ${ENDPOINT}?${PARAMS:-} > $DIR/curl.out
  if [ 0 != $? ] ; then
      errorMsg "ERROR: failed query request"
      exit 2
  fi
  assert_http_status 200 $DIR/headers.out

  if [ ! -f $archive_file ] ; then
      errorMsg "ERROR: output file does not exist"
      exit 2
  fi
  file $archive_file | egrep -q 'archive data'
  if [ $? != 0 ] ; then
      file $archive_file 1>&2
      errorMsg "Expected 'archive data' file contents"
      exit 2
  fi

}
assert_archive_contents(){
  local archive_file=$1;shift
  local count=$1;shift
  local elist=$1;shift
  local EID=
  unzip -l $archive_file > $DIR/archive_list.output
  for EID in $elist ; do
    cat $DIR/archive_list.output | grep -q "executions/execution-$EID.xml" || fail "Archive missing expected file: execution-$EID.xml"
    cat $DIR/archive_list.output | grep -q "executions/output-$EID.rdlog" || fail "Archive missing expected file: output-$EID.rdlog"
    cat $DIR/archive_list.output | grep -q "executions/state-$EID.state.json" || fail "Archive missing expected file: state-$EID.state.json"
  done
  
  local repcount=$(cat $DIR/archive_list.output | grep -e 'reports/report-.*.xml' | wc -l | cut -d' ' -f8)
  if [ "$repcount" != "$count" ] ; then
    fail "report xml expected $count files"
  fi

  local exmlcount=$(cat $DIR/archive_list.output | grep -e 'executions/execution-.*.xml' | wc -l | cut -d' ' -f8)
  if [ "$exmlcount" != "$count" ] ; then
    fail "execution xml expected $count files"
  fi

  # require project.properties not present
  set +e
  cat $DIR/archive_list.output | grep -q "files/etc/project.properties" && fail "Unexpected archive contents: files/etc/project.properties"
  set -e
}


test_archive_executions(){
  local test_proj=$1;shift

  #produce job.xml content corresponding to the dispatch request
  cat > $DIR/temp.out <<END
  <joblist>
     <job>
        <name>cli job</name>
        <group>api-test</group>
        <description></description>
        <loglevel>INFO</loglevel>
        <dispatch>
          <threadcount>1</threadcount>
          <keepgoing>true</keepgoing>
        </dispatch>
        <sequence>
          <command>
          <exec>echo hi</exec>
          </command>
        </sequence>
     </job>
  </joblist>

END
  
  JOBID=$(uploadJob "$test_proj" "$DIR/temp.out")

  #run job to completion twice
  EXECID=$(runJob $JOBID)
  rd-queue follow -q -e $EXECID || fail "rd-queue failed to wait for job completion $EXECID"


  #run job to completion twice
  EXECID2=$(runJob $JOBID)
  rd-queue follow -q -e $EXECID2 || fail "rd-queue failed to wait for job completion $EXECID2"
  
  echo "TEST: Export specifying executions, 2"
  
  get_archive $test_proj $DIR/test_archive.zip "executionIds=$EXECID,$EXECID2"

  #Archive contents
  assert_archive_contents "$DIR/test_archive.zip" "2" "$EXECID $EXECID2"
  
  echo "OK"
  
  echo "TEST: Export specifying executions, 1"
  
  get_archive $test_proj $DIR/test_archive.zip "executionIds=$EXECID"

  #Archive contents
  assert_archive_contents "$DIR/test_archive.zip" "1" "$EXECID"
  
  echo "OK"

  echo "TEST: Export specifying executions, 1"
  
  get_archive $test_proj $DIR/test_archive.zip "executionIds=$EXECID2"

  #Archive contents
  assert_archive_contents "$DIR/test_archive.zip" "1" "$EXECID2"
  
  echo "OK"
}


main(){
  create_project "Test_projectExportExecutions1"
  test_archive_executions "Test_projectExportExecutions1"
  delete_project "Test_projectExportExecutions1"
}


main

rm $DIR/proj_create.post
rm $DIR/curl.out
rm $DIR/test_archive.zip
