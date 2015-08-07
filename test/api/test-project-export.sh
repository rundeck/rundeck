#!/bin/bash

#test GET /api/11/project/name/export
#using API v11

# use api V11
API_VERSION=11
API_XML_NO_WRAPPER=true

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

# now submit req
runurl="${APIURL}/projects"

##
# SETUP: create project, import job
##

test_proj="APIExportTest"
if [ -d $RDECK_PROJECTS/$test_proj ] ; then
  rm -rf $RDECK_PROJECTS/$test_proj
fi

cat > $DIR/proj_create.post <<END
<project>
    <name>$test_proj</name>
    <description>test1</description>
    <config>
        <property key="test.property" value="test value"/>
    </config>
</project>
END

# post
docurl -X POST -D $DIR/headers.out --data-binary @$DIR/proj_create.post -H Content-Type:application/xml ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed POST request"
    exit 2
fi
assert_http_status 201 $DIR/headers.out

#escape the string for xml
xmlargs=$($XMLSTARLET esc "echo hi")

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
        <exec>$xmlargs</exec>
        </command>
      </sequence>
   </job>
</joblist>

END

# now submit req
runurl="${APIURL}/jobs/import"


params="project=$test_proj"

# specify the file for upload with curl, named "xmlBatch"
ulopts="-F xmlBatch=@$DIR/temp.out"
API_XML_NO_WRAPPER=false
# get listing
 docurl $ulopts  ${runurl}?${params} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi

$SHELL $SRC_DIR/api-test-success.sh $DIR/curl.out || exit 2

runurl="${APIURL}/project/$test_proj/export"

echo "TEST: GET $runurl"

docurl -D $DIR/headers.out -o $DIR/test_archive.zip  ${runurl} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed query request"
    exit 2
fi
assert_http_status 200 $DIR/headers.out

if [ ! -f $DIR/test_archive.zip ] ; then
    errorMsg "ERROR: output file does not exist"
    exit 2
fi
file $DIR/test_archive.zip | egrep -q 'archive data'
if [ $? != 0 ] ; then
    file $DIR/test_archive.zip 1>&2
    errorMsg "Expected 'archive data' file contents"
    exit 2
fi

echo "OK"


# now delete the test project

runurl="${APIURL}/project/$test_proj"
docurl -X DELETE  ${runurl} > $DIR/curl.out
if [ 0 != $? ] ; then
    errorMsg "ERROR: failed DELETE request"
    exit 2
fi


rm $DIR/proj_create.post
rm $DIR/curl.out
rm $DIR/test_archive.zip
