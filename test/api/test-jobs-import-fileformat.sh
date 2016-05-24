#!/bin/bash

#test /api/jobs/import using yaml formatted content, use fileformat param, accept json response

DIR=$(cd `dirname $0` && pwd)
source $DIR/include.sh

project=$2
if [ "" == "$2" ] ; then
    project="test"
fi

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
  description: '$0'
  group: api-test
  name: test import yaml
END

# now submit req
ENDPOINT="${APIURL}/project/$project/jobs/import"

test_begin "import Jobs in yaml content with fileformat param and json response"

PARAMS="fileformat=yaml"
POSTFILE="$DIR/temp.out"
TYPE="application/yaml"
ACCEPT="application/json"
METHOD="POST"
EXPECT_STATUS=200

api_request $ENDPOINT $DIR/curl.out

assert_json_value '0' '.failed | length' $DIR/curl.out
assert_json_value '1' '.succeeded | length' $DIR/curl.out
assert_json_value '0' '.skipped | length' $DIR/curl.out

test_succeed


cat > $DIR/temp.out <<END
<joblist>
   <job>
      <name>test import xml</name>
      <group>api-test</group>
      <description>$0</description>
      <loglevel>INFO</loglevel>
      <context>
          <project>$project</project>
      </context>
      <dispatch>
        <threadcount>1</threadcount>
        <keepgoing>true</keepgoing>
      </dispatch>
      <sequence>
        <command>
        <exec>echo test</exec>
        </command>
      </sequence>
   </job>
</joblist>

END

# now submit req
ENDPOINT="${APIURL}/project/$project/jobs/import"

test_begin "import Jobs in XML content with fileformat param and json response"

PARAMS="fileformat=xml"
POSTFILE="$DIR/temp.out"
TYPE="application/xml"
ACCEPT="application/json"
METHOD="POST"
EXPECT_STATUS=200

api_request $ENDPOINT $DIR/curl.out

assert_json_value '0' '.failed | length' $DIR/curl.out
assert_json_value '1' '.succeeded | length' $DIR/curl.out
assert_json_value '0' '.skipped | length' $DIR/curl.out

test_succeed

rm $DIR/curl.out

